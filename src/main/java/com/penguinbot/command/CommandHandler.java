package com.penguinbot.command;

public interface CommandHandler {
    String commandName();
    String description();
    void handle(CommandContext context);
}
