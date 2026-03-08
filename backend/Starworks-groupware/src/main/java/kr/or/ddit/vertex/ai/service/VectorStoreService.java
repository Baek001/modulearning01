package kr.or.ddit.vertex.ai.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Service
public class VectorStoreService {

    @Value("${spring.ai.vectorstore.file:./vector-store.json}")
    private String vectorStoreFilePath;

    private final VertexAiTextEmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final TextSplitter textSplitter;

    public VectorStoreService(VertexAiTextEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = SimpleVectorStore.builder(this.embeddingModel).build();
        this.textSplitter = TokenTextSplitter.builder().withChunkSize(500).build();
    }

    @PostConstruct
    public void initVectorStore() throws IOException {
        log.info("RAG 문서 인덱싱 시작...");
        File vectorStoreFile = new File(vectorStoreFilePath);

        if (vectorStoreFile.exists()) {
            ((SimpleVectorStore) vectorStore).load(vectorStoreFile);
            log.info("벡터 저장소 로드 완료");
        }
    }

    public void indexDocument(String documentContent, Map<String, Object> metadata) {
        Document document = new Document(documentContent, metadata);
        List<Document> chunks = textSplitter.split(document);
        vectorStore.add(chunks);
        log.info("문서 인덱싱 완료: {} 개 청크", chunks.size());
    }

    public List<Document> searchSimilarDocuments(String query, int topK, double threshold) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(threshold)
            .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    public void updateDocument(String documentId, String newContent) {
        vectorStore.delete(Arrays.asList(documentId));
        indexDocument(newContent, Map.of("id", documentId));
    }
}

