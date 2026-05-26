package com.mszlu.ai.knowledge.service;

import com.alibaba.cloud.ai.document.TextDocumentParser;
import com.alibaba.cloud.ai.parser.markdown.MarkdownDocumentParser;
import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.google.gson.JsonObject;
import com.mszlu.ai.common.utils.FileUtils;
import com.mszlu.ai.common.utils.JsonUtils;
import com.mszlu.ai.knowledge.config.MilvusProperties;
import com.mszlu.ai.knowledge.entity.KnowledgeBase;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorService {
    private final MilvusProperties milvusProperties;
    //提取文档
    public List<Document> extractDocument(Resource resource) throws Exception {
        String filename = resource.getFilename();
        if (filename == null){
            throw new IllegalArgumentException("无法获取文件名");
        }
        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        switch ( extension){
            case "md":
                return new MarkdownDocumentParser().parse(resource.getInputStream());
            case "txt":
                return new TextDocumentParser().parse(resource.getInputStream());
            default:
                return new TikaDocumentParser().parse(resource.getInputStream());
        }
    }
    // transform阶段
    public List<Document> transformDocuments(List<Document> documents,
                                            DocumentTransformer transformer) {
        return transformer.transform(documents);
    }

    //load阶段
    public void loadDocuments(List<Document> documents,
                              MilvusServiceClient milvusClient,
                              EmbeddingModel embeddingModel,
                              KnowledgeBase kb)  {
        String collectionName = buildCollectionName(kb.getId());
        String databaseName = milvusProperties.getDatabaseName();
        int batchSize = 10;

        for (int i = 0; i< documents.size(); i+= batchSize){
            int endIndex = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, endIndex);
            //计算embedding
            List<String> texts = batch.stream().map(Document::getText).toList();
            EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(texts);
            List<float[]> embeddings = embeddingResponse.getResults().stream()
                    .map(Embedding::getOutput).toList();
            //构建字段数据
            List<String> ids = new ArrayList<>();
            List<String> parentIds = new ArrayList<>();
            List<String> docIds = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();
            List<JsonObject> metadataList = new ArrayList<>();
            for (int j = 0; j < batch.size(); j++){
                Document doc = batch.get(j);
                Map<String, Object> meta = doc.getMetadata();
                ids.add(UUID.randomUUID().toString());
                parentIds.add(getMetadataString(meta, "parent_id"));
                docIds.add(getMetadataString(meta, "doc_id"));
                contents.add(doc.getText());
                vectors.add(floatArrayToList(embeddings.get(j)));
                metadataList.add(JsonUtils.mapToJsonObject(meta));
            }
            //构建InsertParam
            List<InsertParam.Field> fields = new ArrayList<>();
//            fields.add(new InsertParam.Field("id", ids));
            fields.add(new InsertParam.Field("parent_id", parentIds));
            fields.add(new InsertParam.Field("doc_id", docIds));
            fields.add(new InsertParam.Field("content", contents));
            fields.add(new InsertParam.Field("vector", vectors));
            fields.add(new InsertParam.Field("metadata", metadataList));
            InsertParam insertParam = InsertParam.newBuilder()
                    .withDatabaseName(databaseName)
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();
            //执行插入
            R<MutationResult> insertResult = milvusClient.insert(insertParam);
            if (insertResult.getStatus() != R.Status.Success.getCode()){
                log.error("插入数据失败: {}", insertResult.getMessage());
                throw new RuntimeException("插入数据失败");
            }
        }
    }

    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    private String getMetadataString(Map<String, Object> meta, String key) {
        Object value = meta.get(key);
        return value == null ? "" : value.toString();
    }
    //创建milvus的collection
    public void ensureCollectionExists(MilvusServiceClient milvusClient,
                                       String collectionName,
                                       int embeddingDimension) {
       //检查collection是否存在
        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(milvusProperties.getDatabaseName())
                .build();
        R<Boolean> hasResponse = milvusClient.hasCollection(hasParam);
        if (hasResponse.getData()){
            log.info("collection {} 已经存在", collectionName);
            return;
        }
        log.info("创建collection {}", collectionName);
        //定义schema
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDescription("id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();
        FieldType parentIdField = FieldType.newBuilder()
                .withName("parent_id")
                .withDescription("parent_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .build();
        FieldType docIdField = FieldType.newBuilder()
                .withName("doc_id")
                .withDescription("doc_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .build();
        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDescription("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(8192)
                .build();
        FieldType vectorField = FieldType.newBuilder()
                .withName("vector")
                .withDescription("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(embeddingDimension * 2)
                .build();
        FieldType metadataField = FieldType.newBuilder()
                .withName("metadata")
                .withDescription("metadata")
                .withDataType(DataType.JSON)
                .build();
        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withDatabaseName(milvusProperties.getDatabaseName())
                .withCollectionName(collectionName)
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withSchema(CollectionSchemaParam.newBuilder()
                        .addFieldType(idField)
                        .addFieldType(parentIdField)
                        .addFieldType(docIdField)
                        .addFieldType(contentField)
                        .addFieldType(vectorField)
                        .addFieldType(metadataField)
                        .build())
                .withDescription("collection for knowledge base")
                .build();
        R<RpcStatus> createCollectionResponse = milvusClient.createCollection(createCollectionParam);
        if (createCollectionResponse.getStatus() != R.Status.Success.getCode()){
            log.error("创建collection失败: {}", createCollectionResponse.getMessage());
            throw new RuntimeException("创建collection失败");
        }
        //创建HNSW索引
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(milvusProperties.getDatabaseName())
                .withFieldName("vector")
                .withMetricType(MetricType.COSINE)
                .withIndexType(IndexType.HNSW)
                .withExtraParam("{\"M\": 16,\"efConstruction\":200}")
                .withSyncMode(true)
                .build();
        R<RpcStatus> createIndex = milvusClient.createIndex(createIndexParam);
        if (createIndex.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("创建索引失败", createIndex.getException());
        }
        log.info("创建索引成功");
        //加载collection到内存
        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                        .withDatabaseName(milvusProperties.getDatabaseName())
                .build());
    }

    public VectorStore vectorStore(MilvusServiceClient milvusClient,
                                   EmbeddingModel embeddingModel,
                                   KnowledgeBase kb) {
        String collectionName = buildCollectionName(kb.getId());
        ensureCollectionExists(milvusClient, collectionName, kb.getEmbeddingDimension());
        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .collectionName(collectionName)
                .databaseName(milvusProperties.getDatabaseName())
                .iDFieldName("id")
                .contentFieldName("content")
                .metadataFieldName("metadata")
                .embeddingFieldName("vector")
                .embeddingDimension(kb.getEmbeddingDimension())
                .metricType(MetricType.COSINE)
                .indexType(IndexType.HNSW)
                .initializeSchema(false)
                .autoId(false)
                .build();
    }
    private String buildCollectionName(UUID id) {
        return "kb_" + id.toString().replace("-", "_");
    }

    public void loadCollection(MilvusServiceClient milvusServiceClient, UUID kbId) {
        String collectionName = buildCollectionName(kbId);
        //先检查是否存在这个collection
        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(milvusProperties.getDatabaseName())
                .build();
        R<Boolean> hasResponse = milvusServiceClient.hasCollection(hasParam);
        if (!hasResponse.getData()){
            log.info("collection {}不存在", collectionName);
            return;
        }
        //先load collection
        try {
            //这是异步的，我们可以提前load collection
            R<RpcStatus> rpcStatusR = milvusServiceClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDatabaseName(milvusProperties.getDatabaseName())
                    .build());
            if (rpcStatusR.getStatus() != R.Status.Success.getCode()) {
                log.error("load collection {} error : {}", collectionName, rpcStatusR.getMessage());
            }
        }catch (Exception e) {
            log.warn("load collection {} error", collectionName, e);
        }
    }
    public void deleteDocumentVectors(MilvusServiceClient milvusServiceClient,
                                      UUID kbId,
                                      UUID docId) {
        String collectionName = buildCollectionName(kbId);
        String databaseName = milvusProperties.getDatabaseName();
        //先检查是否存在这个collection
        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(milvusProperties.getDatabaseName())
                .build();
        R<Boolean> hasResponse = milvusServiceClient.hasCollection(hasParam);
        if (!hasResponse.getData()){
            log.info("collection {}不存在", collectionName);
            return;
        }
        String deleteExpr = String.format("doc_id == '%s'", docId.toString());
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(databaseName)
                .withExpr(deleteExpr)
                .build();
        R<MutationResult> deleteResult = milvusServiceClient.delete(deleteParam);
        if (deleteResult.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to delete document vectors from Milvus: " + deleteResult.getMessage());
        }
    }

    public void deleteCollection(MilvusServiceClient milvusServiceClient, UUID kbId) {
        String collectionName = buildCollectionName(kbId);
        String databaseName = milvusProperties.getDatabaseName();
        //先释放collection
        try {
            milvusServiceClient.releaseCollection(
                    ReleaseCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withDatabaseName(databaseName)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to drop collection: " + collectionName, e);
        }

        DropCollectionParam dropCollectionParam = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDatabaseName(databaseName)
                .build();
        R<RpcStatus> rpcStatusR = milvusServiceClient.dropCollection(dropCollectionParam);
        if (rpcStatusR.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("drop collection failed");
        }

    }
}
