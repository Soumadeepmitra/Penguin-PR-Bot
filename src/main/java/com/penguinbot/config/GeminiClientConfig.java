package com.penguinbot.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    @Bean
    public Client geminiClient(AppConfig appConfig) {
        return Client.builder()
            .apiKey(appConfig.getGemini().getApiKey())
            .build();
    }
}
