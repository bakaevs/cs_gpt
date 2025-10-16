package com.cattlescan.systemassistant.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for:
 *  - Splitting long text into chunks
 *  - Requesting embeddings from OpenAI
 */
@Service
public class EmbeddingService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Splits text into chunks of given size (in characters).
     */
    public String[] splitText(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChunkSize, text.length());
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks.toArray(new String[0]);
    }

    /**
     * Calls OpenAI API to create an embedding for the given text chunk.
     *
     * @param content Text to embed
     * @return double[] representing the embedding vector
     * @throws IOException on network or API error
     */
    public double[] generateEmbedding(String content) throws IOException {
        JSONObject body = new JSONObject();
        body.put("model", "text-embedding-3-large");
        body.put("input", content);

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), body.toString());

        Request request = new Request.Builder()
                .url(EMBEDDING_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("OpenAI API error: " + response.code() + " " + response.message());
        }

        String json = response.body().string();
        JSONObject jsonObject = new JSONObject(json);
        JSONArray embeddingArray = jsonObject
                .getJSONArray("data")
                .getJSONObject(0)
                .getJSONArray("embedding");

        double[] embedding = new double[embeddingArray.length()];
        for (int i = 0; i < embeddingArray.length(); i++) {
            embedding[i] = embeddingArray.getDouble(i);
        }

        return embedding;
    }
}
