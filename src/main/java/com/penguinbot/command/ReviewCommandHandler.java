package com.penguinbot.command;

import com.penguinbot.ai.GeminiService;
import com.penguinbot.ai.dto.InlineReviewComment;
import com.penguinbot.ai.dto.ReviewResult;
import com.penguinbot.config.AppConfig;
import com.penguinbot.config.RepoConfig;
import com.penguinbot.config.RepoConfigService;
import com.penguinbot.github.DiffParser;
import com.penguinbot.github.GitHubApiService;
import org.kohsuke.github.ReactionContent;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(ReviewCommandHandler.class);
    
    private final GitHubApiService gitHubApiService;
    private final GeminiService geminiService;
    private final RepoConfigService repoConfigService;
    private final AppConfig appConfig;
    
    public ReviewCommandHandler(GitHubApiService gitHubApiService, GeminiService geminiService,
                                RepoConfigService repoConfigService, AppConfig appConfig) {
        this.gitHubApiService = gitHubApiService;
        this.geminiService = geminiService;
        this.repoConfigService = repoConfigService;
        this.appConfig = appConfig;
    }
    
    @Override 
    public String commandName() { 
        return "review"; 
    }
    
    @Override 
    public String description() { 
        return "AI-powered code review of the PR diff"; 
    }
    
    @Override
    public void handle(CommandContext context) {
        long statusCommentId = -1;
        try {
            // 1. Add 👀 reaction to the triggering comment if present
            if (context.triggeringCommentId() > 0) {
                gitHubApiService.addReaction(context.installationId(), context.repoOwner(), context.repoName(), context.triggeringCommentId(), ReactionContent.EYES);
            }
            
            // 2. Post placeholder comment
            statusCommentId = gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "🐧 **Penguin PR Bot** is reviewing this PR...");
            
            // 3. Load repo config for custom instructions and ignore patterns
            GHRepository repo = gitHubApiService.getRepository(context.installationId(), context.repoOwner(), context.repoName());
            RepoConfig repoConfig = repoConfigService.loadConfig(repo);
            
            // 4. Fetch PR diff and file list
            String diff = gitHubApiService.getPullRequestDiff(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber());
            List<String> files = gitHubApiService.getPullRequestFiles(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber());
            String commitSha = gitHubApiService.getPullRequestHeadSha(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber());
            
            // 5. Call Gemini for structured review (with inline comments for critical actions and files)
            ReviewResult reviewResult = geminiService.reviewCodeStructured(diff, String.join("\n", files), repoConfig.getReviewInstructions());
            
            // 6. Filter and align inline comments with the valid PR diff hunks
            List<InlineReviewComment> validInlineComments = DiffParser.filterAndAlignComments(
                    reviewResult.getComments(),
                    diff,
                    files
            );

            String summary = reviewResult.getSummary() != null ? reviewResult.getSummary() : "Completed PR review.";
            
            // If any comments could not be placed inline, append them to the review summary so feedback is never lost
            StringBuilder fullSummary = new StringBuilder(summary);
            int unplacedCount = (reviewResult.getComments() != null ? reviewResult.getComments().size() : 0) - validInlineComments.size();
            if (unplacedCount > 0) {
                fullSummary.append("\n\n### 📝 Additional Observations\n");
                for (InlineReviewComment c : reviewResult.getComments()) {
                    boolean wasPlaced = validInlineComments.stream().anyMatch(v -> v.getBody().equals(c.getBody()));
                    if (!wasPlaced) {
                        fullSummary.append("- **").append(c.getPath()).append("** (Line ").append(c.getLine()).append("): ").append(c.getBody()).append("\n");
                    }
                }
            }

            String formattedSummary = "## 🐧 Penguin PR Bot — Code Review\n\n"
                    + (validInlineComments.isEmpty()
                        ? ""
                        : "> 💡 *Posted **" + validInlineComments.size() + "** inline review comment(s) directly on the modified lines in the **Files changed** tab.*\n\n")
                    + fullSummary
                    + "\n\n---\n*Reviewed by " + appConfig.getBot().getName() + " using Google Gemini AI*";
            
            // 7. Post GitHub PR review with inline comments on code file changes
            gitHubApiService.createPullRequestReview(
                    context.installationId(),
                    context.repoOwner(),
                    context.repoName(),
                    context.issueNumber(),
                    commitSha,
                    formattedSummary,
                    validInlineComments
            );
            
            // 8. Delete placeholder comment to avoid duplicate review comments in the PR conversation
            if (statusCommentId != -1) {
                try {
                    gitHubApiService.deleteComment(context.installationId(), context.repoOwner(), context.repoName(), statusCommentId);
                } catch (Exception e) {
                    log.debug("Failed to delete placeholder comment #{}: {}", statusCommentId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error handling review command", e);
            if (statusCommentId != -1) {
                gitHubApiService.updateComment(context.installationId(), context.repoOwner(), context.repoName(), statusCommentId, "⚠️ **Penguin PR Bot** encountered an error while reviewing the PR: " + e.getMessage());
            } else {
                gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "⚠️ **Penguin PR Bot** encountered an error while reviewing the PR: " + e.getMessage());
            }
        }
    }
}
