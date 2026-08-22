package com.penguinbot.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penguinbot.command.CommandParser;
import com.penguinbot.config.RepoConfigService;
import com.penguinbot.github.GitHubApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {
    @Autowired 
    MockMvc mockMvc;
    
    @MockBean 
    WebhookSignatureVerifier signatureVerifier;
    
    @MockBean 
    CommandParser commandParser;
    
    @MockBean 
    GitHubApiService gitHubApiService;
    
    @MockBean 
    WebhookEventProcessor eventProcessor;
    
    @MockBean 
    RepoConfigService repoConfigService;
    
    @MockBean 
    ObjectMapper objectMapper;
    
    @Test
    void shouldRejectInvalidSignature() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(false);
        
        mockMvc.perform(post("/webhook")
                .header("X-Hub-Signature-256", "invalid")
                .header("X-GitHub-Event", "ping")
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void shouldAcceptValidWebhook() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(true);
        
        mockMvc.perform(post("/webhook")
                .header("X-Hub-Signature-256", "valid")
                .header("X-GitHub-Event", "issues")
                .content("{}"))
                .andExpect(status().isOk());
    }
    
    @Test
    void shouldReturn200ForUnknownEvent() throws Exception {
        when(signatureVerifier.isValid(any(), any())).thenReturn(true);
        
        mockMvc.perform(post("/webhook")
                .header("X-Hub-Signature-256", "valid")
                .header("X-GitHub-Event", "unknown_event_type")
                .content("{}"))
                .andExpect(status().isOk());
    }
}
