package com.penguinbot.command;

public record CommandContext(
    String commandName,
    String commandArgs,
    long installationId,
    String repoOwner,
    String repoName,
    int issueNumber,
    long triggeringCommentId,
    String senderLogin
) {}
