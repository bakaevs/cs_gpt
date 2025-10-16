package com.cattlescan.systemassistant.embedding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cattlescan.systemassistant.service.EmbeddingService;
import com.cattlescan.systemassistant.service.VectorStoreService;
import com.cattlescan.systemassistant.util.PdfTextExtractor;

/**
 * Handles the full pipeline:
 *
 * 1. Saves uploaded PDF temporarily.
 * 2. Extracts text using PdfTextExtractor.
 * 3. Splits text into chunks for embedding.
 * 4. Generates embeddings for each chunk.
 * 5. Saves embeddings and text in SQL via VectorStoreService.
 */
@Component
public class SystemEmbedding {

    @Autowired
    private PdfTextExtractor pdfTextExtractor;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * Processes the uploaded PDF and stores its embeddings in the database.
     *
     * @param pdfFile the uploaded PDF file
     * @throws Exception if any step fails
     */
    public void processPdf(MultipartFile pdfFile) throws Exception {
        // Step 1: Save the uploaded PDF temporarily
        File tempFile = File.createTempFile("uploaded_", ".pdf");

        try (InputStream in = pdfFile.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        try {
            // Step 2: Extract text
            String pdfText = pdfTextExtractor.extractText(tempFile);
            if (pdfText == null || pdfText.trim().isEmpty()) {
                throw new IllegalArgumentException("PDF contains no readable text");
            }

            // Step 3: Split text into smaller chunks (500 characters each)
            String[] chunks = embeddingService.splitText(pdfText, 500);

            // Step 4: Generate embeddings for each chunk and save to DB
            for (String chunk : chunks) {
                try {
                    double[] embedding = embeddingService.generateEmbedding(chunk);
                    vectorStoreService.saveEmbedding(chunk, embedding);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to process chunk: " + e.getMessage());
                }
            }

        } finally {
            // Step 5: Always clean up temporary file
            if (tempFile.exists() && !tempFile.delete()) {
                System.err.println("⚠️ Temporary file could not be deleted: " + tempFile.getAbsolutePath());
            }
        }
    }
    
    public void processPdfFile(File pdfFile) throws Exception {

        try {
            // Step 2: Extract text
            String pdfText = pdfTextExtractor.extractText(pdfFile);
            if (pdfText == null || pdfText.trim().isEmpty()) {
                throw new IllegalArgumentException("PDF contains no readable text");
            }

            // Step 3: Split text into smaller chunks (500 characters each)
            String[] chunks = embeddingService.splitText(pdfText, 500);

            // Step 4: Generate embeddings for each chunk and save to DB
            for (String chunk : chunks) {
                try {
                    double[] embedding = embeddingService.generateEmbedding(chunk);
                    vectorStoreService.saveEmbedding(chunk, embedding);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to process chunk: " + e.getMessage());
                }
            }

        } finally {

        }
    }    
}
