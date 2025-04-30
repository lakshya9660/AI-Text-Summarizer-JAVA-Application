package com.textsummarizer.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfProcessor {
    
    public static String extractTextFromPdf(File pdfFile) throws IOException {
        if (!pdfFile.exists()) {
            throw new IOException("PDF file does not exist: " + pdfFile.getPath());
        }

        if (pdfFile.length() == 0) {
            throw new IOException("PDF file is empty: " + pdfFile.getPath());
        }

        // Try direct PDDocument loading first
        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (document.isEncrypted()) {
                throw new IOException("Cannot process encrypted PDF files");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            if (text == null || text.trim().isEmpty()) {
                throw new IOException("No text content found in PDF file");
            }
            
            return text;
        } catch (IOException e) {
            // If direct loading fails, try with more detailed error reporting
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pdfFile))) {
                byte[] header = new byte[1024]; // Read more bytes for better detection
                int bytesRead = bis.read(header);
                
                if (bytesRead > 0) {
                    String content = new String(header, 0, bytesRead);
                    if (!content.contains("%PDF")) {
                        // Try to identify the actual file type
                        String fileType = "unknown";
                        if (content.startsWith("PK")) {
                            fileType = "ZIP or Office document";
                        } else if (content.startsWith("{") || content.startsWith("[")) {
                            fileType = "JSON or text file";
                        } else if (content.contains("<!DOCTYPE") || content.contains("<html")) {
                            fileType = "HTML file";
                        }
                        throw new IOException("The selected file appears to be a " + fileType + 
                            ". Please make sure you select a valid PDF file.");
                    }
                }
            }
            
            // If we can't determine the file type, throw the original error
            throw new IOException("Failed to process PDF file. Error: " + e.getMessage() + 
                "\nPlease ensure the file is a valid PDF document and not corrupted.");
        }
    }

    public static String extractTextFromPdf(String pdfPath) throws IOException {
        if (pdfPath == null || pdfPath.trim().isEmpty()) {
            throw new IOException("PDF path cannot be null or empty");
        }
        return extractTextFromPdf(new File(pdfPath));
    }
} 