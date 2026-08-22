package com.penguinbot.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequestPayload {
    private String action;
    private int number;
    @JsonProperty("pull_request")
    private PullRequest pullRequest;
    private WebhookPayload.Repository repository;
    private WebhookPayload.Sender sender;
    private WebhookPayload.Installation installation;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public WebhookPayload.Repository getRepository() {
        return repository;
    }

    public void setRepository(WebhookPayload.Repository repository) {
        this.repository = repository;
    }

    public WebhookPayload.Sender getSender() {
        return sender;
    }

    public void setSender(WebhookPayload.Sender sender) {
        this.sender = sender;
    }

    public WebhookPayload.Installation getInstallation() {
        return installation;
    }

    public void setInstallation(WebhookPayload.Installation installation) {
        this.installation = installation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        private int number;
        private String title;
        private String body;
        private String diff_url;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getDiff_url() {
            return diff_url;
        }

        public void setDiff_url(String diff_url) {
            this.diff_url = diff_url;
        }
    }
}
