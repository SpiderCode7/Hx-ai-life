package com.hxy.hxailife.config;

import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentRetrieverConfig {
    @Bean
    public VectorStoreDocumentRetriever documentRetriever(VectorStore vectorStore) {
        // 1. 直接 return，省略多余的局部变量声明
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore) // 自动注入刚才配置好的 ES VectorStore
                .similarityThreshold(0.3) // 相似度阈值
                .topK(6)                  // 最大返回条数
                .build();
    }
}
