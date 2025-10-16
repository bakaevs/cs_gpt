package com.cattlescan.systemassistant.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EmbeddingUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a double[] array to a JSON string for database storage.
     *
     * @param embedding the array of doubles (e.g., an embedding vector)
     * @return a JSON string representation (e.g., "[0.12, -0.45, 0.78]")
     */
    public static String doubleArrayToJson(double[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert double[] to JSON", e);
        }
    }

    /**
     * Converts a JSON string from the database back into a double[] array.
     *
     * @param json the JSON string (e.g., "[0.12, -0.45, 0.78]")
     * @return the parsed double[] array
     */
    public static double[] jsonToDoubleArray(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON to double[]", e);
        }
    }
}