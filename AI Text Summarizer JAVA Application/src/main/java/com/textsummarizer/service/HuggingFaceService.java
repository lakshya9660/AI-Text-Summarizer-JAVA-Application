package com.textsummarizer.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HuggingFaceService {
    private static final String API_URL = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";
    private static final String API_KEY = "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";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000; // 2 seconds
    private static final int MAX_CHUNK_LENGTH = 500; // Maximum words per chunk
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public HuggingFaceService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public String summarizeText(String text, String style, String perspective) throws IOException {
        // Split text into chunks if it's too long
        List<String> chunks = splitIntoChunks(text);
        
        if (chunks.size() == 1) {
            // If text is short enough, process it directly
            return processSingleChunk(chunks.get(0), style, perspective);
        } else {
            // Process each chunk and combine the summaries
            StringBuilder combinedSummary = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkSummary = processSingleChunk(chunks.get(i), style, perspective);
                if (i > 0) {
                    combinedSummary.append("\n\n");
                }
                combinedSummary.append("Part ").append(i + 1).append(":\n");
                combinedSummary.append(chunkSummary);
            }
            return formatSummary(combinedSummary.toString(), style, perspective);
        }
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        if (words.length <= MAX_CHUNK_LENGTH) {
            chunks.add(text);
            return chunks;
        }

        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;
        int sentenceCount = 0;
        
        for (String word : words) {
            currentChunk.append(word).append(" ");
            wordCount++;
            
            // Check for sentence endings
            if (word.endsWith(".") || word.endsWith("!") || word.endsWith("?")) {
                sentenceCount++;
            }
            
            // Create a new chunk when we reach the word limit or have enough sentences
            if (wordCount >= MAX_CHUNK_LENGTH || sentenceCount >= 5) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                wordCount = 0;
                sentenceCount = 0;
            }
        }
        
        // Add the last chunk if there's any remaining text
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }

    private String processSingleChunk(String text, String style, String perspective) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
                return trySummarizeText(text);
            } catch (IOException e) {
                lastException = e;
                if (!shouldRetry(e)) {
                    throw e;
                }
                if (attempt == MAX_RETRIES - 1) {
                    throw new IOException("Failed to generate summary after " + MAX_RETRIES + 
                        " attempts. Last error: " + e.getMessage(), e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Operation interrupted", e);
            }
        }
        
        throw lastException;
    }

    private String trySummarizeText(String text) throws IOException {
        String requestBody = objectMapper.writeValueAsString(new SummarizationRequest(text));

        Request request = new Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details available";
                String errorMessage = String.format("API request failed: %d - %s", 
                    response.code(), errorBody);
                
                if (response.code() == 503) {
                    errorMessage += "\nThe service is temporarily unavailable. Please try again later.";
                } else if (response.code() == 429) {
                    errorMessage += "\nToo many requests. Please wait a moment before trying again.";
                }
                
                throw new IOException(errorMessage);
            }

            String responseBody = response.body().string();
            try {
                List<SummarizationResponse> responses = objectMapper.readValue(responseBody,
                    new TypeReference<List<SummarizationResponse>>() {});
                
                if (responses == null || responses.isEmpty()) {
                    throw new IOException("Empty response from API");
                }
                
                SummarizationResponse summarizationResponse = responses.get(0);
                if (summarizationResponse.getSummaryText() == null) {
                    throw new IOException("No summary text in response");
                }
                
                return summarizationResponse.getSummaryText();
            } catch (Exception e) {
                throw new IOException("Failed to parse API response: " + responseBody, e);
            }
        }
    }

    private boolean shouldRetry(IOException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("503") || // Service Unavailable
               message.contains("429") || // Too Many Requests
               message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("temporarily unavailable");
    }

    private String formatSummary(String summary, String style, String perspective) {
        StringBuilder formattedSummary = new StringBuilder();

        // Add perspective header
        formattedSummary.append("Perspective: ").append(perspective).append("\n\n");

        // Apply style formatting
        switch (style) {
            case "Bullet Point Summary":
                String[] sentences = summary.split("\\. ");
                for (String sentence : sentences) {
                    if (!sentence.trim().isEmpty()) {
                        formattedSummary.append("• ").append(sentence.trim()).append("\n");
                    }
                }
                break;
            case "Executive Summary":
                formattedSummary.append("EXECUTIVE SUMMARY\n");
                formattedSummary.append("================\n\n");
                formattedSummary.append(summary);
                break;
            case "Technical Summary":
                formattedSummary.append("TECHNICAL SUMMARY\n");
                formattedSummary.append("================\n\n");
                formattedSummary.append(summary);
                break;
            default: // Standard Summary
                formattedSummary.append(summary);
        }

        return formattedSummary.toString();
    }

    private static class SummarizationRequest {
        private final String inputs;

        public SummarizationRequest(String inputs) {
            this.inputs = inputs;
        }

        public String getInputs() {
            return inputs;
        }
    }

    private static class SummarizationResponse {
        private String summary_text;

        public SummarizationResponse() {
            // Default constructor for Jackson
        }

        public String getSummaryText() {
            return summary_text;
        }

        public void setSummary_text(String summary_text) {
            this.summary_text = summary_text;
        }
    }
} 