package com.penguinbot.github;

import com.penguinbot.ai.dto.InlineReviewComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DiffParser {
    private static final Logger log = LoggerFactory.getLogger(DiffParser.class);

    public static Map<String, Set<Integer>> getValidRightSideLines(String diff) {
        Map<String, Set<Integer>> fileLines = new HashMap<>();
        if (diff == null || diff.isBlank()) {
            return fileLines;
        }

        String currentFile = null;
        int currentNewLine = -1;
        boolean inHunk = false;

        String[] lines = diff.split("\r?\n");
        for (String line : lines) {
            if (line.startsWith("diff --git ")) {
                currentFile = null;
                inHunk = false;
            } else if (line.startsWith("+++ b/")) {
                currentFile = normalizePath(line.substring(6).trim());
                fileLines.putIfAbsent(currentFile, new HashSet<>());
            } else if (line.startsWith("@@") && currentFile != null) {
                // e.g. @@ -1,5 +10,12 @@ or @@ -1 +1 @@
                inHunk = true;
                int plusIdx = line.indexOf("+");
                if (plusIdx != -1) {
                    int afterPlus = plusIdx + 1;
                    int endIdx = afterPlus;
                    while (endIdx < line.length() && Character.isDigit(line.charAt(endIdx))) {
                        endIdx++;
                    }
                    if (endIdx > afterPlus) {
                        try {
                            currentNewLine = Integer.parseInt(line.substring(afterPlus, endIdx));
                        } catch (NumberFormatException e) {
                            inHunk = false;
                        }
                    } else {
                        inHunk = false;
                    }
                }
            } else if (inHunk && currentFile != null) {
                if (line.startsWith("+")) {
                    fileLines.get(currentFile).add(currentNewLine);
                    currentNewLine++;
                } else if (line.startsWith(" ")) {
                    fileLines.get(currentFile).add(currentNewLine);
                    currentNewLine++;
                } else if (line.startsWith("-")) {
                    // deleted line on LEFT side; does not advance new file line counter
                } else if (line.startsWith("\\ No newline at end of file")) {
                    // ignore
                } else {
                    // end of hunk
                    inHunk = false;
                }
            }
        }
        return fileLines;
    }

    public static List<InlineReviewComment> filterAndAlignComments(List<InlineReviewComment> comments, String diff, List<String> availableFiles) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Set<Integer>> validDiffLines = getValidRightSideLines(diff);
        List<InlineReviewComment> alignedComments = new ArrayList<>();

        for (InlineReviewComment comment : comments) {
            if (comment == null || comment.getBody() == null || comment.getBody().isBlank()) {
                continue;
            }

            String matchedPath = resolveFilePath(comment.getPath(), validDiffLines.keySet(), availableFiles);
            if (matchedPath == null) {
                log.warn("Skipping inline comment for unmatched file path: {}", comment.getPath());
                continue;
            }

            Set<Integer> validLines = validDiffLines.getOrDefault(matchedPath, Collections.emptySet());
            if (validLines.isEmpty()) {
                log.warn("No valid diff lines found for file: {}", matchedPath);
                continue;
            }

            int targetLine = comment.getLine();
            if (validLines.contains(targetLine)) {
                alignedComments.add(new InlineReviewComment(
                        matchedPath,
                        targetLine,
                        "RIGHT",
                        comment.getSeverity(),
                        comment.getBody()
                ));
            } else {
                // Try to find the closest valid line in the diff hunk within 10 lines
                Integer closest = findClosestLine(validLines, targetLine, 10);
                if (closest != null) {
                    log.info("Aligned comment from line {} to nearest diff line {} for {}", targetLine, closest, matchedPath);
                    alignedComments.add(new InlineReviewComment(
                            matchedPath,
                            closest,
                            "RIGHT",
                            comment.getSeverity(),
                            comment.getBody()
                    ));
                } else {
                    log.warn("Line {} for file {} is not in PR diff; skipping inline comment", targetLine, matchedPath);
                }
            }
        }

        return alignedComments;
    }

    private static String resolveFilePath(String rawPath, Set<String> diffPaths, List<String> availableFiles) {
        if (rawPath == null) return null;
        String normalized = normalizePath(rawPath);

        // Exact match
        for (String p : diffPaths) {
            if (p.equalsIgnoreCase(normalized)) return p;
        }

        // Suffix match (e.g. if rawPath is "Service.java" and diff path is "src/main/java/Service.java")
        for (String p : diffPaths) {
            if (p.endsWith(normalized) || normalized.endsWith(p)) return p;
        }

        if (availableFiles != null) {
            for (String p : availableFiles) {
                String np = normalizePath(p);
                if (np.equalsIgnoreCase(normalized) || np.endsWith(normalized) || normalized.endsWith(np)) {
                    return p;
                }
            }
        }
        return null;
    }

    private static Integer findClosestLine(Set<Integer> validLines, int targetLine, int maxDistance) {
        Integer closest = null;
        int minDiff = Integer.MAX_VALUE;

        for (int line : validLines) {
            int diff = Math.abs(line - targetLine);
            if (diff < minDiff && diff <= maxDistance) {
                minDiff = diff;
                closest = line;
            }
        }
        return closest;
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        String p = path.trim().replace('\\', '/');
        while (p.startsWith("./")) {
            p = p.substring(2);
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.startsWith("b/")) {
            p = p.substring(2);
        }
        if (p.startsWith("a/")) {
            p = p.substring(2);
        }
        return p;
    }
}
