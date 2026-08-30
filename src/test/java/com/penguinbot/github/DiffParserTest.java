package com.penguinbot.github;

import com.penguinbot.ai.dto.InlineReviewComment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DiffParserTest {

    private final String sampleDiff = """
            diff --git a/src/main/java/com/example/OrderService.java b/src/main/java/com/example/OrderService.java
            index 1234567..89abcdef 100644
            --- a/src/main/java/com/example/OrderService.java
            +++ b/src/main/java/com/example/OrderService.java
            @@ -10,6 +10,8 @@ package com.example;
             public class OrderService {
             -    private int oldField;
             +    private String orderId;
             +    private double amount;
                  public void process() {
             """;

    @Test
    void testGetValidRightSideLines() {
        Map<String, Set<Integer>> validLines = DiffParser.getValidRightSideLines(sampleDiff);
        assertTrue(validLines.containsKey("src/main/java/com/example/OrderService.java"));
        Set<Integer> lines = validLines.get("src/main/java/com/example/OrderService.java");
        
        // Lines in hunk: 10 (context), 11 (added orderId), 12 (added amount), 13 (context)
        assertTrue(lines.contains(10));
        assertTrue(lines.contains(11));
        assertTrue(lines.contains(12));
        assertTrue(lines.contains(13));
        assertFalse(lines.contains(100));
    }

    @Test
    void testFilterAndAlignCommentsExactMatch() {
        List<InlineReviewComment> rawComments = List.of(
                new InlineReviewComment("src/main/java/com/example/OrderService.java", 11, "RIGHT", "CRITICAL", "🔴 Field should be final.")
        );

        List<InlineReviewComment> aligned = DiffParser.filterAndAlignComments(rawComments, sampleDiff, List.of("src/main/java/com/example/OrderService.java"));
        assertEquals(1, aligned.size());
        assertEquals(11, aligned.get(0).getLine());
        assertEquals("src/main/java/com/example/OrderService.java", aligned.get(0).getPath());
    }

    @Test
    void testFilterAndAlignCommentsFuzzyPathAndLineAlignment() {
        // Comment with relative filename and line 9 (near line 10)
        List<InlineReviewComment> rawComments = List.of(
                new InlineReviewComment("OrderService.java", 9, "RIGHT", "WARNING", "🟡 Check formatting.")
        );

        List<InlineReviewComment> aligned = DiffParser.filterAndAlignComments(rawComments, sampleDiff, List.of("src/main/java/com/example/OrderService.java"));
        assertEquals(1, aligned.size());
        assertEquals(10, aligned.get(0).getLine()); // Aligned to line 10
        assertEquals("src/main/java/com/example/OrderService.java", aligned.get(0).getPath());
    }
}
