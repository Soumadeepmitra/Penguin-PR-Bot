package com.penguinbot.command;

import com.penguinbot.github.GitHubApiService;
import org.kohsuke.github.ReactionContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LabelCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(LabelCommandHandler.class);
    
    private final GitHubApiService gitHubApiService;
    
    public LabelCommandHandler(GitHubApiService gitHubApiService) {
        this.gitHubApiService = gitHubApiService;
    }
    
    @Override 
    public String commandName() { 
        return "label"; 
    }
    
    @Override 
    public String description() { 
        return "Add a label to the PR (usage: /label <label-name>)"; 
    }
    
    @Override
    public void handle(CommandContext context) {
        try {
            String labelName = context.commandArgs();
            if (labelName == null || labelName.isBlank()) {
                gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "Usage: `/label <label-name>`");
                return;
            }
            
            gitHubApiService.addLabel(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), labelName.trim());
            gitHubApiService.addReaction(context.installationId(), context.repoOwner(), context.repoName(), context.triggeringCommentId(), ReactionContent.PLUS_ONE);
        } catch (Exception e) {
            log.error("Error handling label command", e);
            gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "⚠️ Failed to add label: " + e.getMessage());
        }
    }
}
