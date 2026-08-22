package com.penguinbot.command;

import com.penguinbot.ai.GeminiService;
import com.penguinbot.config.AppConfig;
import com.penguinbot.github.GitHubApiService;
import org.kohsuke.github.ReactionContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SummarizeCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(SummarizeCommandHandler.class);
    
    private final GitHubApiService gitHubApiService;
    private final GeminiService geminiService;
    private final AppConfig appConfig;
    
    public SummarizeCommandHandler(GitHubApiService gitHubApiService, GeminiService geminiService, AppConfig appConfig) {
        this.gitHubApiService = gitHubApiService;
        this.geminiService = geminiService;
        this.appConfig = appConfig;
    }
    
    @Override 
    public String commandName() { 
        return "summarize"; 
    }
    
    @Override 
    public String description() { 
        return "Generate a concise summary of the PR changes"; 
    }
    
    @Override
    public void handle(CommandContext context) {
        long statusCommentId = -1;
        try {
            if (context.triggeringCommentId() > 0) {
                gitHubApiService.addReaction(context.installationId(), context.repoOwner(), context.repoName(), context.triggeringCommentId(), ReactionContent.EYES);
            }
            statusCommentId = gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "🐧 **Penguin PR Bot** is summarizing this PR...");
            
            String diff = gitHubApiService.getPullRequestDiff(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber());
            List<String> files = gitHubApiService.getPullRequestFiles(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber());
            
            String summary = geminiService.summarizePR(diff, String.join("\n", files));
            
            String formatted = formatSummary(summary);
            gitHubApiService.updateComment(context.installationId(), context.repoOwner(), context.repoName(), statusCommentId, formatted);
        } catch (Exception e) {
            log.error("Error handling summarize command", e);
            if (statusCommentId != -1) {
                gitHubApiService.updateComment(context.installationId(), context.repoOwner(), context.repoName(), statusCommentId, "⚠️ **Penguin PR Bot** encountered an error: " + e.getMessage());
            } else {
                gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "⚠️ **Penguin PR Bot** encountered an error: " + e.getMessage());
            }
        }
    }
    
    private String formatSummary(String summary) {
        return "## 🐧 Penguin PR Bot — PR Summary\n\n" + summary + 
               "\n\n---\n*Summarized by " + appConfig.getBot().getName() + " using Google Gemini AI*";
    }
}
