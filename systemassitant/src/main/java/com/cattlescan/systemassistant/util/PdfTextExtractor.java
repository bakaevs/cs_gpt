package com.cattlescan.systemassistant.util;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extracts plain text from a PDF file using Apache PDFBox.
 *
 * <p>Usage:
 * PdfTextExtractor extractor = new PdfTextExtractor();
 * String text = extractor.extractText(new File("example.pdf"));
 */
@Component
public class PdfTextExtractor {

    /**
     * Extracts all readable text from a PDF file.
     *
     * @param pdfFile the PDF file
     * @return the extracted text, or an empty string if unreadable
     */
    public String extractText(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            System.err.println("⚠️ PDF file not found: " + (pdfFile != null ? pdfFile.getAbsolutePath() : "null"));
            return "";
        }

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Keeps text order consistent
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        } catch (IOException e) {
            System.err.println("⚠️ Error extracting text from PDF: " + e.getMessage());
            return "";
        }
    }
}
