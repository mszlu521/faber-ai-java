package com.mszlu.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mszlu.ai.knowledge.entity.Document;
import com.mszlu.ai.knowledge.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
