package com.penguinbot.ai;

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

@Service
public class GeminiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    private final Client geminiClient;
    private final AppConfig appConfig;
    private final String reviewSystemPrompt;
    private final String summarizeSystemPrompt;
    
    public GeminiService(Client geminiClient, AppConfig appConfig) {
        this.geminiClient = geminiClient;
        this.appConfig = appConfig;
        this.reviewSystemPrompt = loadPrompt("prompts/review-system.txt");
        this.summarizeSystemPrompt = loadPrompt("prompts/summarize-system.txt");
    }
    
    public String reviewCode(String diff, String fileList, String customInstructions) {
        String systemPrompt = reviewSystemPrompt;
        if (customInstructions != null && !customInstructions.isBlank()) {
            systemPrompt += "\n\nAdditional Review Instructions:\n" + customInstructions;
        }
        String userPrompt = "## Files Changed\n" + fileList + "\n\n## Diff\n```diff\n" + diff + "\n```";
        return callGemini(systemPrompt, userPrompt);
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
