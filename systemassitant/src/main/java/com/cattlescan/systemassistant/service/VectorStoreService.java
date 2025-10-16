package com.cattlescan.systemassistant.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
 * Handles storage and semantic retrieval of text embeddings in SQL Server.
 *
 * Table schema example:
 * CREATE TABLE Embeddings (
 *     id INT IDENTITY PRIMARY KEY,
 *     text NVARCHAR(MAX),
 *     embedding NVARCHAR(MAX)
 * );
 */
@Service
public class VectorStoreService {

	public static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);
	
	@Autowired
	private EmbeddingRepository embeddingRepository;

    /**
     * Saves a text fragment and its embedding into SQL Server.
     */
    public void saveEmbedding(String text, double[] embedding) throws SQLException {
    	embeddingRepository.save(new Embedding(text, EmbeddingUtils.doubleArrayToJson(embedding)));
    }

    /**
     * Performs semantic search based on cosine similarity.
     * Retrieves stored embeddings and compares them with the query vector.
     */
    public List<SearchResult> findSimilar(double[] queryEmbedding) {
        List<SearchResult> results = new ArrayList<>();

        try {
            Iterable<Embedding> allEmbeddings = embeddingRepository.findAll();

            for (Embedding record : allEmbeddings) {
                if (record.getEmbedding() == null) continue;

                double[] storedEmbedding = EmbeddingUtils.jsonToDoubleArray(record.getEmbedding());
                double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);
                results.add(new SearchResult(record.getText(), similarity));
            }

        } catch (Exception e) {
        	logger.error("⚠️ Error during vector search: " + e.getMessage(), e);
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
     * Converts a float vector to a comma-separated string for database storage.
     */
    private String vectorToString(double[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    /**
     * Parses a stored string into a float vector.
     */
    private double[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) return new double[0];

        String[] parts = vectorStr.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                vector[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                vector[i] = 0f;
            }
        }
        return vector;
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
