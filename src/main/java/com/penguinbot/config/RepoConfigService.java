package com.penguinbot.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.springframework.stereotype.Service;

@Service
public class RepoConfigService {
    private static final String CONFIG_FILE = ".penguin-bot.yml";
    private final ObjectMapper yamlMapper;
    
    public RepoConfigService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    public RepoConfig loadConfig(GHRepository repository) {
        try {
            GHContent content = repository.getFileContent(CONFIG_FILE);
            return yamlMapper.readValue(content.read(), RepoConfig.class);
        } catch (Exception e) {
            // File doesn't exist or can't be parsed — return defaults
            return RepoConfig.defaultConfig();
        }
    }
}
