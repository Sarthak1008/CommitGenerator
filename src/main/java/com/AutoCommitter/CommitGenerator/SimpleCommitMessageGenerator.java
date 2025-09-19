package com.AutoCommitter.CommitGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.AutoCommitter.CommitGenerator.Git.StagedDiffService;

/**
 * Heuristic conventional commit message generator from staged diffs.
 */
public class SimpleCommitMessageGenerator {

    public String generate(List<StagedDiffService.StagedDiff> diffs) {
        if (diffs == null || diffs.isEmpty()) {
            return "chore: no staged changes";
        }

        Map<String, Integer> changeCounters = new HashMap<>();
        changeCounters.put("ADD", 0);
        changeCounters.put("MODIFY", 0);
        changeCounters.put("DELETE", 0);
        changeCounters.put("RENAME", 0);

        StringJoiner affected = new StringJoiner(", ");

        for (StagedDiffService.StagedDiff diff : diffs) {
            String type = normalizeType(diff.changeType);
            changeCounters.put(type, changeCounters.getOrDefault(type, 0) + 1);
            affected.add(shortenPath(diff.filePath));
        }

        String scope = inferScopeFromFiles(diffs);
        String type = inferType(changeCounters);

        String summary = type + (scope.isEmpty() ? "" : "(" + scope + ")") + ": " + summarize(changeCounters, affected.toString());

        String body = buildBody(diffs);

        return body.isEmpty() ? summary : summary + "\n\n" + body;
    }

    private String inferType(Map<String, Integer> counters) {
        int adds = counters.getOrDefault("ADD", 0);
        int mods = counters.getOrDefault("MODIFY", 0);
        int dels = counters.getOrDefault("DELETE", 0);
        if (adds > 0 && mods == 0 && dels == 0) return "feat";
        if (mods > 0 && adds == 0 && dels == 0) return "fix";
        if (adds + mods + dels > 5) return "refactor";
        return "chore";
    }

    private String inferScopeFromFiles(List<StagedDiffService.StagedDiff> diffs) {
        String first = diffs.get(0).filePath;
        int slash = first.indexOf('/');
        if (slash > 0) {
            return first.substring(0, slash);
        }
        return "";
    }

    private String summarize(Map<String, Integer> counters, String affectedList) {
        StringJoiner parts = new StringJoiner(", ");
        if (counters.getOrDefault("ADD", 0) > 0) parts.add(counters.get("ADD") + " added");
        if (counters.getOrDefault("MODIFY", 0) > 0) parts.add(counters.get("MODIFY") + " changed");
        if (counters.getOrDefault("DELETE", 0) > 0) parts.add(counters.get("DELETE") + " removed");
        if (counters.getOrDefault("RENAME", 0) > 0) parts.add(counters.get("RENAME") + " renamed");
        String counts = parts.toString();
        return (counts.isEmpty() ? "update files" : counts) + ": " + affectedList;
    }

    private String shortenPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String normalizeType(String changeType) {
        if ("ADD".equalsIgnoreCase(changeType) || "ADD\n".equalsIgnoreCase(changeType)) return "ADD";
        if ("DELETE".equalsIgnoreCase(changeType)) return "DELETE";
        if ("RENAME".equalsIgnoreCase(changeType)) return "RENAME";
        return "MODIFY";
    }

    private String buildBody(List<StagedDiffService.StagedDiff> diffs) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (StagedDiffService.StagedDiff diff : diffs) {
            if (diff.patch == null || diff.patch.isEmpty()) continue;
            sb.append("- ").append(diff.filePath).append("\n");
            String[] lines = diff.patch.split("\n");
            int count = 0;
            for (String line : lines) {
                if (line.startsWith("+") || line.startsWith("-")) {
                    sb.append("  ").append(line).append("\n");
                    count++;
                }
                if (count >= 5) break; // limit per file
            }
            shown++;
            if (shown >= 5) break; // limit files in body
        }
        return sb.toString().trim();
    }
}


