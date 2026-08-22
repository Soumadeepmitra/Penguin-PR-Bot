package com.penguinbot.command;

import com.penguinbot.github.GitHubApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HelpCommandHandler implements CommandHandler {
    private static final Logger log = LoggerFactory.getLogger(HelpCommandHandler.class);
    
    private final GitHubApiService gitHubApiService;
    private final List<CommandHandler> allHandlers;
    
    public HelpCommandHandler(GitHubApiService gitHubApiService, List<CommandHandler> allHandlers) {
        this.gitHubApiService = gitHubApiService;
        this.allHandlers = allHandlers;
    }
    
    @Override 
    public String commandName() { 
        return "help"; 
    }
    
    @Override 
    public String description() { 
        return "Show available bot commands"; 
    }
    
    @Override
    public void handle(CommandContext context) {
        try {
            StringBuilder helpMessage = new StringBuilder();
            helpMessage.append("## 🐧 Penguin PR Bot — Help\n\n");
            helpMessage.append("| Command | Description |\n");
            helpMessage.append("|---|---|\n");
            
            for (CommandHandler handler : allHandlers) {
                helpMessage.append("| `/").append(handler.commandName()).append("` | ")
                           .append(handler.description()).append(" |\n");
            }
            
            helpMessage.append("\n*Powered by Google Gemini AI*");
            
            gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), helpMessage.toString());
        } catch (Exception e) {
            log.error("Error handling help command", e);
            gitHubApiService.postComment(context.installationId(), context.repoOwner(), context.repoName(), context.issueNumber(), "⚠️ Failed to display help: " + e.getMessage());
        }
    }
}
