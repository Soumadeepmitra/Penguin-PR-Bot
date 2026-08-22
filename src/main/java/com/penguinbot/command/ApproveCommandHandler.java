package com.penguinbot.command;

import com.penguinbot.github.GitHubApiService;
import org.kohsuke.github.ReactionContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApproveCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(ApproveCommandHandler.class);
    
    private final GitHubApiService gitHubApiService;
    
    public ApproveCommandHandler(GitHubApiService gitHubApiService) {
        this.gitHubApiService = gitHubApiService;
    }
    
    @Override 
    public String commandName() { 
        return "approve"; 
    }
    
    @Override 
    public String description() { 
        return "Approve the pull request with an optional message"; 
    }
    
    @Override
    public void handle(CommandContext context) {
        try {
            String message = context.commandArgs();
            if (message == null || message.isBlank()) {
                message = "Approved by Penguin PR Bot 🐧";
            }
            
            gitHubApiService.approvePullRequest(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), message);
            gitHubApiService.addReaction(context.installationId(), context.repoOwner(), context.repoName(), context.triggeringCommentId(), ReactionContent.PLUS_ONE);
            gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "🐧 PR approved!");
        } catch (Exception e) {
            log.error("Error handling approve command", e);
            gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "⚠️ Failed to approve PR: " + e.getMessage());
        }
    }
}
