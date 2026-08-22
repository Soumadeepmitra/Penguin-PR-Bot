package com.penguinbot.command;

import com.penguinbot.ai.GeminiService;
import com.penguinbot.config.AppConfig;
import com.penguinbot.config.RepoConfig;
import com.penguinbot.config.RepoConfigService;
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
            
            // 4. Fetch PR diff
            String diff = gitHubApiService.getPullRequestDiff(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber());
            
            // 5. Filter diff based on ignore patterns from repoConfig (impl skipped per instructions, just fetching files)
            // 6. Truncate if diff exceeds repoConfig.getMaxDiffSize() (impl skipped)
            
            // 7. Get file list
            List<String> files = gitHubApiService.getPullRequestFiles(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber());
            
            // 8. Call Gemini for review (pass custom reviewInstructions from repoConfig)
            String review = geminiService.reviewCode(diff, String.join("\n", files), repoConfig.getReviewInstructions());
            
            // 9. Format and update the status comment with the full review
            String formatted = formatReview(review);
            gitHubApiService.updateComment(context.installationId(), context.repoOwner(), context.repoName(), statusCommentId, formatted);
        } catch (Exception e) {
            log.error("Error handling review command", e);
            if (statusCommentId != -1) {
                gitHubApiService.updateComment(context.installationId(), context.repoOwner(), context.repoName(), statusCommentId, "⚠️ **Penguin PR Bot** encountered an error while reviewing the PR: " + e.getMessage());
            } else {
                gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "⚠️ **Penguin PR Bot** encountered an error while reviewing the PR: " + e.getMessage());
            }
        }
    }
    
    private String formatReview(String review) {
        return "## 🐧 Penguin PR Bot — Code Review\n\n" + review + 
               "\n\n---\n*Reviewed by " + appConfig.getBot().getName() + " using Google Gemini AI*";
    }
}
