package com.penguinbot.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueCommentPayload {
    private String action;
    private Comment comment;
    private Issue issue;
    private WebhookPayload.Repository repository;
    private WebhookPayload.Sender sender;
    private WebhookPayload.Installation installation;

    public boolean isPullRequest() {
        return issue != null && issue.getPullRequest() != null;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public Issue getIssue() {
        return issue;
    }

    public void setIssue(Issue issue) {
        this.issue = issue;
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
    public static class Comment {
        private long id;
        private String body;
        @JsonProperty("user")
        private WebhookPayload.Sender user;
        @JsonProperty("html_url")
        private String htmlUrl;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public WebhookPayload.Sender getUser() {
            return user;
        }

        public void setUser(WebhookPayload.Sender user) {
            this.user = user;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public void setHtmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
        private int number;
        private String title;
        @JsonProperty("pull_request")
        private PullRequestRef pullRequest;

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

        public PullRequestRef getPullRequest() {
            return pullRequest;
        }

        public void setPullRequest(PullRequestRef pullRequest) {
            this.pullRequest = pullRequest;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequestRef {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
