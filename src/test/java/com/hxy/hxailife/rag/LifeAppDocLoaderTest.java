package com.hxy.hxailife.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LifeAppDocLoaderTest {

    @Autowired
    private LifeAppDocLoader lifeAppDocLoader;

    @Test
    void loadMardowns() {
        List<Document> list = lifeAppDocLoader.loadMardowns();
        System.out.println(list.toString());
    }
}