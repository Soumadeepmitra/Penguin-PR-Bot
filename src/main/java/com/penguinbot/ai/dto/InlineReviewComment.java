package com.penguinbot.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InlineReviewComment {
    private String path;
    private int line;
    private String side = "RIGHT";
    private String severity; // CRITICAL, WARNING, SUGGESTION
    private String body;

    public InlineReviewComment() {}

    public InlineReviewComment(String path, int line, String side, String severity, String body) {
        this.path = path;
        this.line = line;
        this.side = side != null ? side : "RIGHT";
        this.severity = severity;
        this.body = body;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getSide() {
        return side != null ? side : "RIGHT";
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
