package com.lijay.lijayaiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AppDocumentLoaderTest {
    @Autowired
    private AppDocumentLoader appDocumentLoader;
    @Test
    void loadMarkdown() {
        List<Document> documents = appDocumentLoader.loadMarkdown();

    }
}