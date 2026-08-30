package com.penguinbot.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.penguinbot.ai.dto.ReviewResult;
import com.penguinbot.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GeminiServiceTest {
    private Client mockClient;
    private Models mockModels;
    private AppConfig mockAppConfig;
    private AppConfig.Gemini mockGeminiConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        mockClient = Mockito.mock(Client.class);
        mockModels = Mockito.mock(Models.class);
        
        java.lang.reflect.Field modelsField = Client.class.getDeclaredField("models");
        modelsField.setAccessible(true);
        modelsField.set(mockClient, mockModels);
        
        mockAppConfig = Mockito.mock(AppConfig.class);
        mockGeminiConfig = Mockito.mock(AppConfig.Gemini.class);
        when(mockAppConfig.getGemini()).thenReturn(mockGeminiConfig);
        when(mockGeminiConfig.getModel()).thenReturn("gemini-3.6-flash");
        objectMapper = new ObjectMapper();
    }

    @Test
    void testReviewCode() throws Exception {
        GeminiService geminiService = new GeminiService(mockClient, mockAppConfig, objectMapper);
        
        GenerateContentResponse mockResponse = Mockito.mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("AI Review Output");
        when(mockModels.generateContent(eq("gemini-3.6-flash"), any(String.class), any(GenerateContentConfig.class)))
                .thenReturn(mockResponse);

        String result = geminiService.reviewCode("diff content", "file1.java", null);
        assertEquals("AI Review Output", result);
    }

    @Test
    void testReviewCodeStructuredWithInlineComments() throws Exception {
        GeminiService geminiService = new GeminiService(mockClient, mockAppConfig, objectMapper);
        
        String jsonPayload = """
                ```json
                {
                  "summary": "Found 1 critical bug in auth logic.",
                  "comments": [
                    {
                      "path": "src/main/Auth.java",
                      "line": 42,
                      "side": "RIGHT",
                      "severity": "CRITICAL",
                      "body": "🔴 **Critical Action**: Null check missing before token validation."
                    }
                  ]
                }
                ```
                """;
        
        GenerateContentResponse mockResponse = Mockito.mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn(jsonPayload);
        when(mockModels.generateContent(eq("gemini-3.6-flash"), any(String.class), any(GenerateContentConfig.class)))
                .thenReturn(mockResponse);

        ReviewResult result = geminiService.reviewCodeStructured("diff content", "src/main/Auth.java", null);
        assertEquals("Found 1 critical bug in auth logic.", result.getSummary());
        assertEquals(1, result.getComments().size());
        assertEquals("src/main/Auth.java", result.getComments().get(0).getPath());
        assertEquals(42, result.getComments().get(0).getLine());
        assertEquals("CRITICAL", result.getComments().get(0).getSeverity());
        assertEquals("🔴 **Critical Action**: Null check missing before token validation.", result.getComments().get(0).getBody());
    }

    @Test
    void testSummarizePR() throws Exception {
        GeminiService geminiService = new GeminiService(mockClient, mockAppConfig, objectMapper);
        
        GenerateContentResponse mockResponse = Mockito.mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("AI Summary Output");
        when(mockModels.generateContent(eq("gemini-3.6-flash"), any(String.class), any(GenerateContentConfig.class)))
                .thenReturn(mockResponse);

        String result = geminiService.summarizePR("diff content", "file1.java");
        assertEquals("AI Summary Output", result);
    }

    @Test
    void testReviewCodeWithCustomInstructions() throws Exception {
        GeminiService geminiService = new GeminiService(mockClient, mockAppConfig, objectMapper);
        
        GenerateContentResponse mockResponse = Mockito.mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("AI Review Output");
        
        ArgumentCaptor<GenerateContentConfig> configCaptor = ArgumentCaptor.forClass(GenerateContentConfig.class);
        when(mockModels.generateContent(eq("gemini-3.6-flash"), any(String.class), configCaptor.capture()))
                .thenReturn(mockResponse);

        geminiService.reviewCode("diff content", "file1.java", "Be extra strict");
        
        GenerateContentConfig capturedConfig = configCaptor.getValue();
        assertTrue(capturedConfig.systemInstruction().isPresent());
        String systemInstructionText = capturedConfig.systemInstruction().get().toString();
        assertTrue(systemInstructionText.contains("Additional Review Instructions:\nBe extra strict"));
    }

    @Test
    void testErrorHandling() throws Exception {
        GeminiService geminiService = new GeminiService(mockClient, mockAppConfig, objectMapper);
        
        when(mockModels.generateContent(any(String.class), any(String.class), any(GenerateContentConfig.class)))
                .thenThrow(new RuntimeException("API Error"));

        String result = geminiService.reviewCode("diff content", "file1.java", null);
        assertEquals("⚠️ Error generating AI response. Please try again later.", result);
    }
}
