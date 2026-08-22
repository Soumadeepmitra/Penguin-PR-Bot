package com.penguinbot.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RepoConfig {
    
    @JsonProperty("autoReview")
    private boolean autoReview = true;
    
    @JsonProperty("autoSummarize")
    private boolean autoSummarize = true;
    
    @JsonProperty("ignore")
    private List<String> ignore = new ArrayList<>();
    
    @JsonProperty("reviewInstructions")
    private String reviewInstructions = "";
    
    @JsonProperty("maxDiffSize")
    private int maxDiffSize = 50000;

    public boolean isAutoReview() { return autoReview; }
    public void setAutoReview(boolean autoReview) { this.autoReview = autoReview; }

    public boolean isAutoSummarize() { return autoSummarize; }
    public void setAutoSummarize(boolean autoSummarize) { this.autoSummarize = autoSummarize; }

    public List<String> getIgnore() { return ignore; }
    public void setIgnore(List<String> ignore) { this.ignore = ignore; }

    public String getReviewInstructions() { return reviewInstructions; }
    public void setReviewInstructions(String reviewInstructions) { this.reviewInstructions = reviewInstructions; }

    public int getMaxDiffSize() { return maxDiffSize; }
    public void setMaxDiffSize(int maxDiffSize) { this.maxDiffSize = maxDiffSize; }
    
    public static RepoConfig defaultConfig() {
        return new RepoConfig();
    }
}
