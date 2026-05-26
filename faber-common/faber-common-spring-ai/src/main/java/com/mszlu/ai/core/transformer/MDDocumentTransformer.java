package com.mszlu.ai.core.transformer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MDDocumentTransformer implements DocumentTransformer {
    //md文档按标题分块转换器
    //识别md标题 # ##这些，按标题层级构建文档树结构 每个标题做为独立的Document
    @Override
    public List<Document> apply(List<Document> documents) {
        if (documents.isEmpty()){
            return List.of();
        }
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            if (doc == null || doc.getText() == null
                    || doc.getText().isEmpty()){
                continue;
            }
            try{
                List<Document> chunks = splitByHeadings(doc);
                result.addAll(chunks);
            }catch (Exception e){
                log.error("Error while processing document: {}", doc.getText(), e);
                result.add(doc);
            }

        }
        return result;
    }
    //标题正则
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    //最小内容长度阈值
    private int minContentLength = 100;
    //最大内容长度阈值
    private int maxContentLength = 5000;
    //是否包含标题路径
    private boolean includeHeadingPath = true;

    public MDDocumentTransformer() {
        this(50, 8000, true);
    }
    public MDDocumentTransformer(int minContentLength,
                                 int maxContentLength,
                                 boolean includeHeadingPath) {
        this.minContentLength = minContentLength;
        this.maxContentLength = maxContentLength;
        this.includeHeadingPath = includeHeadingPath;
    }

    private List<Document> splitByHeadings(Document doc) {
        //按照标题进行分块
        String text = doc.getText();
        List<HeadingBlock> blocks = parseHeadingBlocks(text);
        if (blocks.isEmpty()){
            return List.of(doc);
        }
        List<Document> chunks = new ArrayList<>();
        for (HeadingBlock block : blocks) {
            String content = buildChunkContent(block);
            //如果内容过长，按段落进一步拆分
            if (content.length() > maxContentLength){
                chunks.addAll(splitLargeBlock(block, content,doc.getMetadata()));
            }else{
                chunks.add(createChunkDocument(block , content,doc.getMetadata()));
            }
        }
        return chunks;
    }

    private Collection<? extends Document> splitLargeBlock(HeadingBlock block,
                                                           String content,
                                                           Map<String, Object> originalMetadata) {
        //进一步拆分
        List<Document> subChunks = new ArrayList<>();
        //按段落拆分
        String[] paragraphs = content.split("\n\s*\n");
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        //添加标题前缀
        String headingPrefix = includeHeadingPath ?
                String.join(" > ", block.getHeadingPath()) + " \n\n "
                : block.getRawHeading() + "\n\n";
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()){
                continue;
            }
            if (currentChunk.length() + trimmed.length() > maxContentLength
            && !currentChunk.isEmpty()){
                String chunkContent = headingPrefix + currentChunk.toString().trim();
                subChunks.add(createSubChunkDocument(block, chunkContent, originalMetadata, chunkIndex++));
                currentChunk = new StringBuilder();
            }
            if (!currentChunk.isEmpty()){
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);
        }
        if (!currentChunk.isEmpty()){
            String chunkContent = headingPrefix + currentChunk.toString().trim();
            subChunks.add(createSubChunkDocument(block, chunkContent, originalMetadata, chunkIndex));
        }
        return subChunks;
    }

    private Document createSubChunkDocument(HeadingBlock block,
                                            String chunkContent,
                                            Map<String, Object> originalMetadata,
                                            int chunkIndex) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        if (originalMetadata != null){
            metadata.putAll(originalMetadata);
        }
        metadata.put("chunk_type", "sub_chunk");
        metadata.put("sub_chunk_index", chunkIndex);
        metadata.put("heading_level", block.getLevel());
        metadata.put("heading_title", block.getTitle());
        metadata.put("heading_path", String.join(" > ", block.getHeadingPath()));
        return new Document(chunkContent, metadata);
    }

    private Document createChunkDocument(HeadingBlock block,
                                         String content,
                                         Map<String, Object> originalMetadata) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        if (originalMetadata != null){
            metadata.putAll(originalMetadata);
        }
        metadata.put("heading_level", block.getLevel());
        metadata.put("heading_title", block.getTitle());
        metadata.put("heading_path", String.join(" > ", block.getHeadingPath()));
        metadata.put("chunk_type", block.getLevel() == 0 ? "preamble" : "heading");
        return new Document(content, metadata);
    }

    private String buildChunkContent(HeadingBlock block) {
        //构建分块内容
        if (block.getLevel() == 0){
            return block.getContent();
        }
        StringBuilder sb = new StringBuilder();
        if (includeHeadingPath && !block.getHeadingPath().isEmpty()){
            sb.append(String.join(" > ", block.getHeadingPath()));
            sb.append("\n\n");
        }else{
            sb.append(block.getRawHeading());
            sb.append("\n\n");
        }
        sb.append(block.getContent());
        return sb.toString().trim();
    }

    private List<HeadingBlock> parseHeadingBlocks(String text) {
        List<HeadingBlock> blocks = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(text);
        List<Integer> headingPositions = new ArrayList<>();
        List<Integer> headingLevels = new ArrayList<>();
        List<String> headingTitles = new ArrayList<>();
        while (matcher.find()){
            headingPositions.add(matcher.start());
            headingLevels.add(matcher.group(1).length());
            headingTitles.add(matcher.group(2).trim());
        }
        if (headingPositions.isEmpty()){
            return blocks;
        }
        //处理文档开头到第一个标题之间的内容 做为前言
        if (headingPositions.get(0) > 0){
            String preamble = text.substring(0, headingPositions.get(0)).trim();
            if (!preamble.isEmpty()){
                HeadingBlock block = new HeadingBlock();
                block.setLevel(0);
                block.setTitle("");
                block.setContent(preamble);
                block.setHeadingPath(Collections.emptyList());
                blocks.add(block);
            }
        }
        //提取每个标题块的内容
        for (int i = 0; i < headingPositions.size(); i++){
            int start = headingPositions.get(i);
            int end = (i + 1 < headingPositions.size()) ? headingPositions.get(i + 1) : text.length();
            String blockText = text.substring(start, end).trim();
            String[] lines = blockText.split("\r?\n",2);
            String titleLine = lines[0].trim();
            String content = (lines.length > 1) ? lines[1].trim() : "";
            int level = headingLevels.get(i);
            String title = headingTitles.get(i);
            HeadingBlock block = new HeadingBlock();
            block.setLevel(level);
            block.setTitle(title);
            block.setRawHeading(titleLine);
            block.setContent(content);
            block.setHeadingPath(buildHeadingPath(blocks,level, title));
            blocks.add(block);
        }
        return blocks;
    }

    private List<String> buildHeadingPath(List<HeadingBlock> blocks,
                                          int level,
                                          String title) {
        List<String> path = new ArrayList<>();
        for (int i = blocks.size() - 1; i >= 0; i--){
            HeadingBlock block = blocks.get(i);
            if (block.getLevel() > 0 && block.getLevel() <= level){
                path.addAll(block.getHeadingPath());
                if (!path.contains(block.getTitle())){
                    path.add(block.getTitle());
                }
                break;
            }
        }
        path.add( title);
        return path;
    }

    //标题块
    @Data
    private static class HeadingBlock {
        //标题层级
        int level;
        //标题文本
        String title ;
        //原始标题行
        String rawHeading;
        //标题下内容
        String content;
        //标题路径
        List<String> headingPath;
    }
}
