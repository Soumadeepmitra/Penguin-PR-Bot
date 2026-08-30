package com.penguinbot.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penguinbot.ai.dto.ReviewResult;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.penguinbot.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
public class GeminiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    private final Client geminiClient;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final String reviewSystemPrompt;
    private final String summarizeSystemPrompt;
    
    public GeminiService(Client geminiClient, AppConfig appConfig, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.reviewSystemPrompt = loadPrompt("prompts/review-system.txt");
        this.summarizeSystemPrompt = loadPrompt("prompts/summarize-system.txt");
    }
    
    public ReviewResult reviewCodeStructured(String diff, String fileList, String customInstructions) {
        String systemPrompt = reviewSystemPrompt;
        if (customInstructions != null && !customInstructions.isBlank()) {
            systemPrompt += "\n\nAdditional Review Instructions:\n" + customInstructions;
        }
        String userPrompt = "## Files Changed\n" + fileList + "\n\n## Diff\n```diff\n" + diff + "\n```";
        String rawResponse = callGemini(systemPrompt, userPrompt);
        return parseReviewResult(rawResponse);
    }
    
    public String reviewCode(String diff, String fileList, String customInstructions) {
        ReviewResult result = reviewCodeStructured(diff, fileList, customInstructions);
        return result.getSummary();
    }
    
    public ReviewResult parseReviewResult(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new ReviewResult("No review output generated.", Collections.emptyList());
        }
        if (rawResponse.startsWith("⚠️")) {
            return new ReviewResult(rawResponse, Collections.emptyList());
        }
        
        String cleanJson = rawResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        try {
            ReviewResult result = objectMapper.readValue(cleanJson, ReviewResult.class);
            if (result.getSummary() == null || result.getSummary().isBlank()) {
                result.setSummary(rawResponse);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse Gemini structured JSON review, falling back to markdown summary: {}", e.getMessage());
            return new ReviewResult(rawResponse, Collections.emptyList());
        }
    }
    
    public String summarizePR(String diff, String fileList) {
        String userPrompt = "## Files Changed\n" + fileList + "\n\n## Diff\n```diff\n" + diff + "\n```";
        return callGemini(summarizeSystemPrompt, userPrompt);
    }
    
    private String callGemini(String systemPrompt, String userPrompt) {
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .build();
            
            GenerateContentResponse response = geminiClient.models.generateContent(
                appConfig.getGemini().getModel(),
                userPrompt,
                config
            );
            
            return response.text();
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return "⚠️ Error generating AI response. Please try again later.";
        }
    }
    
    private String loadPrompt(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Prompt file not found: " + resourcePath + ". Using default prompt.");
                return "You are an AI assistant.";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + resourcePath, e);
        }
    }
}
