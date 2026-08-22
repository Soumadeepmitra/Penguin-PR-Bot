package com.penguinbot.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandParserTest {
    private CommandParser commandParser;

    @BeforeEach
    void setUp() {
        commandParser = new CommandParser();
    }

    @Test
    void testParseReviewCommand() {
        Optional<CommandContext> ctx = commandParser.parse("/review", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isPresent());
        assertEquals("review", ctx.get().commandName());
        assertEquals("", ctx.get().commandArgs());
    }

    @Test
    void testParseSummarizeCommand() {
        Optional<CommandContext> ctx = commandParser.parse("/summarize", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isPresent());
        assertEquals("summarize", ctx.get().commandName());
    }

    @Test
    void testParseLabelCommandWithArgs() {
        Optional<CommandContext> ctx = commandParser.parse("/label bug", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isPresent());
        assertEquals("label", ctx.get().commandName());
        assertEquals("bug", ctx.get().commandArgs());
    }

    @Test
    void testParseDeleteCommentsCommand() {
        Optional<CommandContext> ctx = commandParser.parse("/delete-pr-bot-comments", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isPresent());
        assertEquals("delete-pr-bot-comments", ctx.get().commandName());
    }

    @Test
    void testParseHelpCommand() {
        Optional<CommandContext> ctx = commandParser.parse("/help", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isPresent());
        assertEquals("help", ctx.get().commandName());
    }

    @Test
    void testParseApproveCommandWithArgs() {
        Optional<CommandContext> ctx = commandParser.parse("/approve LGTM!", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isPresent());
        assertEquals("approve", ctx.get().commandName());
        assertEquals("LGTM!", ctx.get().commandArgs());
    }

    @Test
    void testParseUnknownCommand() {
        Optional<CommandContext> ctx = commandParser.parse("/unknown", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isEmpty());
    }

    @Test
    void testParseNormalComment() {
        Optional<CommandContext> ctx = commandParser.parse("just a normal comment", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isEmpty());
    }

    @Test
    void testParseEmptyString() {
        Optional<CommandContext> ctx = commandParser.parse("", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isEmpty());
    }

    @Test
    void testParseCommandWithExtraContext() {
        Optional<CommandContext> ctx = commandParser.parse("/review\nsome extra context", 1L, "owner", "repo", 2, 3L, "sender");
        assertTrue(ctx.isPresent());
        assertEquals("review", ctx.get().commandName());
        assertEquals("some extra context", ctx.get().commandArgs());
    }
}
