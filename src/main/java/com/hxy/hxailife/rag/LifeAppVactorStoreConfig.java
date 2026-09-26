package com.hxy.hxailife.rag;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class LifeAppVactorStoreConfig {

    @Bean
    public CommandLineRunner loadLifeAppDocuments(
            VectorStore vectorStore,
            LifeAppDocLoader lifeAppDocLoader) {

        return args -> {
            List<Document> documents = lifeAppDocLoader.loadMardowns();
            int batchSize = 10;

          if(documents != null && documents.size()> 0 ){
                for(int i = 0;i < documents.size();i += batchSize){
                    int end = Math.min(i + batchSize,documents.size());
                    //从集合中取出 i 到 end 的子集
                    List<Document> batch = documents.subList(i,end);
                    vectorStore.add(batch);

                    log.info("成功插入从{}到{}的数据",i,end);
                }
                log.info("知识库载入成功");
            }else {log.info("无需插入");}
        };
    }
}
