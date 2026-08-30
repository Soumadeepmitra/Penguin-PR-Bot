package com.penguinbot.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResult {
    private String summary;
    private List<InlineReviewComment> comments = new ArrayList<>();

    public ReviewResult() {}

    public ReviewResult(String summary, List<InlineReviewComment> comments) {
        this.summary = summary;
        this.comments = comments != null ? comments : new ArrayList<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<InlineReviewComment> getComments() {
        return comments;
    }

    public void setComments(List<InlineReviewComment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
    }
}
