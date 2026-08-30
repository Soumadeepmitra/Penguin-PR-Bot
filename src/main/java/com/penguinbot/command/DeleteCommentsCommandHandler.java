package com.penguinbot.command;

import com.penguinbot.github.GitHubApiService;
import org.kohsuke.github.GHIssueComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeleteCommentsCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(DeleteCommentsCommandHandler.class);
    
    private final GitHubApiService gitHubApiService;
    
    public DeleteCommentsCommandHandler(GitHubApiService gitHubApiService) {
        this.gitHubApiService = gitHubApiService;
    }
    
    @Override 
    public String commandName() { 
        return "delete-pr-bot-comments"; 
    }
    
    @Override 
    public String description() { 
        return "Delete all previous bot comments on this PR"; 
    }
    
    @Override
    public void handle(CommandContext context) {
        try {
            String botLogin = gitHubApiService.getAuthenticatedBotLogin(context.installationId());
            List<GHIssueComment> botComments = gitHubApiService.listBotComments(
                    context.installationId(),
                    context.repoOwner(),
                    context.repoName(),
                    context.issueNumber(),
                    botLogin
            );
            
            int deletedCount = 0;
            for (GHIssueComment comment : botComments) {
                if (comment.getId() != context.triggeringCommentId()) {
                    gitHubApiService.deleteComment(context.installationId(), context.repoOwner(), context.repoName(), comment.getId());
                    deletedCount++;
                }
            }
            
            gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "🐧 Cleaned up " + deletedCount + " bot comment(s).");
        } catch (Exception e) {
            log.error("Error handling delete-pr-bot-comments command", e);
            gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "⚠️ Failed to delete bot comments: " + e.getMessage());
        }
    }
}
