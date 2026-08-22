package com.penguinbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppConfig.class)
@ConfigurationProperties(prefix = "penguin")
public class AppConfig {
    private GitHub github = new GitHub();
    private Gemini gemini = new Gemini();
    private Bot bot = new Bot();
    
    public GitHub getGithub() { return github; }
    public void setGithub(GitHub github) { this.github = github; }
    
    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }
    
    public Bot getBot() { return bot; }
    public void setBot(Bot bot) { this.bot = bot; }

    public static class GitHub {
        private String appId;
        private String privateKeyPath;
        private String webhookSecret;
        
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    }
    
    public static class Gemini {
        private String apiKey;
        private String model = "gemini-2.5-flash";
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
    
    public static class Bot {
        private String name = "Penguin PR Bot";
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
