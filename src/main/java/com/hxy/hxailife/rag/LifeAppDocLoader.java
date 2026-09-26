package com.hxy.hxailife.rag;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.print.Doc;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LifeAppDocLoader {

    private final ApplicationContext applicationContext;

    @Autowired
    private ElasticsearchClient client;

//    private final ResourcePatternResolver resourcePatternResolver;
//
//    public LifeAppDocLoader(ResourcePatternResolver resourcePatternResolver){
//        this.resourcePatternResolver = resourcePatternResolver;
//    }

    /**
     * 加载 MarkDown 文件
     * 获取本地 Markdown 文件的文件名，，然后去 ES查询是否有对应文件信息
     * @return
     */
    public List<Document> loadMardowns() {
        List<Document> allDoc = new ArrayList<>();
        try {
            Resource[] resources = applicationContext.getResources("classpath:doc/*.md");
            System.out.println(resources);
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                SearchResponse<Document> response = client.search(s -> s
                                .index("life-app-documents")
                                .size(1)
                                .query(q -> q
                                        .term(t -> t
                                                // 使用点号访问层级，加上 .keyword 进行精确匹配
                                                .field("metadata.fileName.keyword")
                                                .value(fileName)
                                        )
                                ),
                        Document.class
                );
//                System.out.println("es的数据："+ response);
                if(response.hits().hits().isEmpty()){
                    MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                            .withHorizontalRuleCreateDocument(true) //保持语义的切分
                            .withIncludeCodeBlock(false) //代码块
                            .withIncludeBlockquote(false) //行首带有 > （一个大于号加上一个空格）的格式
                            .withAdditionalMetadata("fileName", fileName)
                            .build();
                    MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                    allDoc.addAll(markdownDocumentReader.get());
                }
            }
        } catch (IOException e) {
            log.error("MarkDown 文档加载失败", e);
        }
        // 文本切分，防止过大数据（最大token大小、最小块字符数、少于xxtoken碎片不向量化、单个原文档允许拆出的最大块数、保留切分分隔符）
        TokenTextSplitter textSplitter = new TokenTextSplitter(800, 100, 5, 10000, true);
        List<Document> documents = textSplitter.apply(allDoc);
        return documents;
    }
}
