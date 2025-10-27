package com.cattlescan.systemassistant.service;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cattlescan.systemassistant.entity.Embedding;
import com.cattlescan.systemassistant.model.SearchResult;
import com.cattlescan.systemassistant.repository.EmbeddingRepository;
import com.cattlescan.systemassistant.util.EmbeddingUtils;

/**
 * In-memory + persistent vector store.
 * 
 * Loads all embeddings from DB into memory once and uses them for fast similarity search.
 */
@Service
public class VectorStoreService {

    public static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);

    @Autowired
    private EmbeddingRepository embeddingRepository;

    /** 
     * Thread-safe in-memory cache of embeddings.
     */
    private final List<Embedding> inMemoryEmbeddings = new CopyOnWriteArrayList<>();

    /**
     * Loads all embeddings into memory (should be called at startup).
     */
    @Autowired
    public void initializeCache() {
        try {
            logger.info("🔄 Loading all embeddings from database into memory...");
            inMemoryEmbeddings.clear();
            Iterable<Embedding> dbEmbeddings = embeddingRepository.findAll();
            for (Embedding e : dbEmbeddings) {
                if (e.getEmbedding() != null)
                    inMemoryEmbeddings.add(e);
            }
            logger.info("✅ Loaded " + inMemoryEmbeddings.size() + " embeddings into memory.");
        } catch (Exception e) {
            logger.error("⚠️ Failed to load embeddings into memory: " + e.getMessage(), e);
        }
    }

    /**
     * Saves a text fragment and its embedding into both DB and memory.
     */
    public synchronized void saveEmbedding(String text, double[] embedding) throws SQLException {
        try {
            Embedding entity = new Embedding(text, EmbeddingUtils.doubleArrayToJson(embedding));
            embeddingRepository.save(entity);
            inMemoryEmbeddings.add(entity);
        } catch (Exception e) {
            logger.error("⚠️ Error saving embedding: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Performs semantic search based on cosine similarity using in-memory cache.
     */
    public List<SearchResult> findSimilar(double[] queryEmbedding) {
        List<SearchResult> results = new ArrayList<>();

        if (inMemoryEmbeddings.isEmpty()) {
            logger.warn("⚠️ No embeddings loaded in memory, falling back to database...");
            initializeCache();
        }

        try {
            for (Embedding record : inMemoryEmbeddings) {
                if (record.getEmbedding() == null) continue;

                double[] storedEmbedding = EmbeddingUtils.jsonToDoubleArray(record.getEmbedding());
                double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);
                results.add(new SearchResult(record.getText(), similarity));
            }
        } catch (Exception e) {
            logger.error("⚠️ Error during in-memory vector search: " + e.getMessage(), e);
        }

        // Sort by descending similarity and return top 5
        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Combines retrieved text fragments into a single context block.
     */
    public String combineResults(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        for (SearchResult r : results) {
            sb.append(r.getText()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Computes cosine similarity between two float vectors.
     */
    private double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;

        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }
}
