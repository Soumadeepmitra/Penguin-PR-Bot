package com.penguinbot.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GitHubApiService {
    private static final Logger log = LoggerFactory.getLogger(GitHubApiService.class);

    private final GitHubAppAuthService authService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GitHubApiService(GitHubAppAuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getPullRequestDiff(long installationId, String owner, String repo, int prNumber) {
        try {
            String token = authService.getInstallationToken(installationId);
            String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d", owner, repo, prNumber);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github.v3.diff")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                log.error("Failed to fetch PR diff: {}", response.body());
                throw new RuntimeException("Failed to fetch PR diff");
            }
        } catch (Exception e) {
            log.error("Error getting PR diff", e);
            throw new RuntimeException("Error getting PR diff", e);
        }
    }

    public List<String> getPullRequestFiles(long installationId, String owner, String repo, int prNumber) {
        try {
            GitHub gitHub = authService.getGitHubClient(installationId);
            GHRepository ghRepo = gitHub.getRepository(owner + "/" + repo);
            GHPullRequest pr = ghRepo.getPullRequest(prNumber);
            return pr.listFiles().toList().stream()
                    .map(GHPullRequestFileDetail::getFilename)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting PR files", e);
            throw new RuntimeException("Error getting PR files", e);
        }
    }

    public long postComment(long installationId, String owner, String repo, int issueNumber, String body) {
        try {
            GitHub gitHub = authService.getGitHubClient(installationId);
            GHRepository ghRepo = gitHub.getRepository(owner + "/" + repo);
            GHIssueComment comment = ghRepo.getIssue(issueNumber).comment(body);
            return comment.getId();
        } catch (Exception e) {
            log.error("Error posting comment", e);
            throw new RuntimeException("Error posting comment", e);
        }
    }

    public void updateComment(long installationId, String owner, String repo, long commentId, String body) {
        try {
            String token = authService.getInstallationToken(installationId);
            String url = String.format("https://api.github.com/repos/%s/%s/issues/comments/%d", owner, repo, commentId);
            String payload = objectMapper.writeValueAsString(Map.of("body", body));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to update comment {}: {}", commentId, response.body());
                throw new RuntimeException("Failed to update comment: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error updating comment {}", commentId, e);
            throw new RuntimeException("Error updating comment", e);
        }
    }

    public List<GHIssueComment> getIssueComments(long installationId, String owner, String repo, int issueNumber) {
        try {
            GitHub gitHub = authService.getGitHubClient(installationId);
            GHRepository ghRepo = gitHub.getRepository(owner + "/" + repo);
            return ghRepo.getIssue(issueNumber).getComments();
        } catch (Exception e) {
            log.error("Error getting issue comments for issue #{}", issueNumber, e);
            throw new RuntimeException("Error getting issue comments", e);
        }
    }

    public List<GHIssueComment> listBotComments(long installationId, String owner, String repo, int issueNumber, String botLogin) {
        try {
            List<GHIssueComment> comments = getIssueComments(installationId, owner, repo, issueNumber);
            return comments.stream()
                    .filter(c -> {
                        try {
                            String login = c.getUser().getLogin();
                            return login.equalsIgnoreCase(botLogin)
                                    || login.equalsIgnoreCase(botLogin + "[bot]")
                                    || login.endsWith("[bot]");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error listing bot comments", e);
            throw new RuntimeException("Error listing bot comments", e);
        }
    }

    public void markPreviousReviewsAsOutdated(long installationId, String owner, String repo, int issueNumber, String botLogin) {
        try {
            List<GHIssueComment> comments = listBotComments(installationId, owner, repo, issueNumber, botLogin);
            for (GHIssueComment comment : comments) {
                String body = comment.getBody();
                if (body != null && body.contains("Code Review") && !body.contains("Outdated Review")) {
                    String updatedBody = "## 🐧 Penguin PR Bot — Code Review\n\n"
                            + "> ⚠️ **Outdated Review** *(New changes/fixes were pushed to this branch)*\n>\n"
                            + "> <details>\n"
                            + "> <summary>🕒 Click to view previous suggestions</summary>\n>\n"
                            + "> " + body.replace("\n", "\n> ") + "\n"
                            + "> </details>";
                    comment.update(updatedBody);
                    log.info("Marked previous review comment #{} on PR #{} as outdated", comment.getId(), issueNumber);
                }
            }
        } catch (Exception e) {
            log.error("Error marking previous reviews as outdated on PR #{}", issueNumber, e);
        }
    }

    public void deleteComment(long installationId, String owner, String repo, long commentId) {
        try {
            String token = authService.getInstallationToken(installationId);
            String url = String.format("https://api.github.com/repos/%s/%s/issues/comments/%d", owner, repo, commentId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github+json")
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204 && response.statusCode() != 200) {
                log.error("Failed to delete comment {}: {}", commentId, response.body());
                throw new RuntimeException("Failed to delete comment: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error deleting comment {}", commentId, e);
            throw new RuntimeException("Error deleting comment", e);
        }
    }

    public void addReaction(long installationId, String owner, String repo, long commentId, ReactionContent reaction) {
        try {
            String token = authService.getInstallationToken(installationId);
            String url = String.format("https://api.github.com/repos/%s/%s/issues/comments/%d/reactions", owner, repo, commentId);
            String payload = objectMapper.writeValueAsString(Map.of("content", reaction.getContent()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("Failed to add reaction to comment {}: {}", commentId, response.body());
                throw new RuntimeException("Failed to add reaction: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Error adding reaction to comment {}", commentId, e);
            throw new RuntimeException("Error adding reaction", e);
        }
    }

    public String getAuthenticatedBotLogin(long installationId) {
        try {
            GitHub gitHub = authService.getGitHubClient(installationId);
            return gitHub.getMyself().getLogin();
        } catch (Exception e) {
            log.error("Error getting authenticated bot login", e);
            throw new RuntimeException("Error getting authenticated bot login", e);
        }
    }

    public void addLabel(long installationId, String owner, String repo, int issueNumber, String label) {
        try {
            GitHub gitHub = authService.getGitHubClient(installationId);
            GHRepository ghRepo = gitHub.getRepository(owner + "/" + repo);
            ghRepo.getIssue(issueNumber).addLabels(label);
        } catch (Exception e) {
            log.error("Error adding label", e);
            throw new RuntimeException("Error adding label", e);
        }
    }

    public void approvePullRequest(long installationId, String owner, String repo, int prNumber, String body) {
        try {
            GitHub gitHub = authService.getGitHubClient(installationId);
            GHRepository ghRepo = gitHub.getRepository(owner + "/" + repo);
            ghRepo.getPullRequest(prNumber).createReview()
                    .event(GHPullRequestReviewEvent.APPROVE)
                    .body(body)
                    .create();
        } catch (Exception e) {
            log.error("Error approving PR", e);
            throw new RuntimeException("Error approving PR", e);
        }
    }

    public GHRepository getRepository(long installationId, String owner, String repo) {
        try {
            GitHub gitHub = authService.getGitHubClient(installationId);
            return gitHub.getRepository(owner + "/" + repo);
        } catch (Exception e) {
            log.error("Error getting repository", e);
            throw new RuntimeException("Error getting repository", e);
        }
    }
}
