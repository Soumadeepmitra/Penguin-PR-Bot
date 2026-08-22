package com.penguinbot.command;

import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CommandParser {
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^\\s*/([a-zA-Z][a-zA-Z0-9-]*)\\s*(.*)", Pattern.DOTALL);
    private static final Set<String> KNOWN_COMMANDS = Set.of(
        "review", "summarize", "delete-pr-bot-comments", "help", "label", "approve"
    );
    
    public Optional<CommandContext> parse(String commentBody, long installationId,
            String repoOwner, String repoName, int issueNumber,
            long triggeringCommentId, String senderLogin) {
        if (commentBody == null) {
            return Optional.empty();
        }
        
        Matcher matcher = COMMAND_PATTERN.matcher(commentBody);
        if (matcher.find()) {
            String commandName = matcher.group(1).toLowerCase();
            if (KNOWN_COMMANDS.contains(commandName)) {
                String commandArgs = matcher.group(2).trim();
                return Optional.of(new CommandContext(
                    commandName,
                    commandArgs,
                    installationId,
                    repoOwner,
                    repoName,
                    issueNumber,
                    triggeringCommentId,
                    senderLogin
                ));
            }
        }
        return Optional.empty();
    }
}
