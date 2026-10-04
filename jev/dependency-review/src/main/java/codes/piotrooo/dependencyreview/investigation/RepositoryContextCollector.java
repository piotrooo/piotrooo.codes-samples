package codes.piotrooo.dependencyreview.investigation;

import codes.piotrooo.dependencyreview.dependency.DependencyUpdate;
import codes.piotrooo.dependencyreview.git.GitDiffReader.GitDiff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RepositoryContextCollector {

    private static final Set<String> SKIPPED = Set.of(".git", "build", "target", "node_modules", ".gradle", ".idea");

    public List<SourceSnippet> collect(Path repository, GitDiff diff, DependencyUpdate update, int maxFiles) {
        Set<String> changed = new HashSet<>(diff.changedFiles());
        List<SearchToken> tokens = searchTokens(update);

        try (var paths = Files.walk(repository)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> allowed(repository, path))
                    .map(path -> candidate(repository, path, changed, tokens))
                    .filter(candidate -> candidate.score > 0)
                    .sorted(Comparator.comparingInt(Candidate::score).reversed())
                    .limit(maxFiles)
                    .map(candidate -> new SourceSnippet(candidate.relativePath, candidate.reason, candidate.content))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot scan repository " + repository, e);
        }
    }

    private Candidate candidate(
            Path repository,
            Path path,
            Set<String> changed,
            List<SearchToken> tokens) {
        String relative = repository.relativize(path).toString().replace('\\', '/');
        try {
            if (Files.size(path) > 200_000) {
                return Candidate.none(relative);
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            int score = 0;
            List<String> reasons = new ArrayList<>();
            if (changed.contains(relative)) {
                score += 10;
                reasons.add("changed file");
            }
            SearchToken firstMatch = null;
            for (SearchToken token : tokens) {
                if (containsToken(content, token.value())) {
                    score += token.score();
                    reasons.add(token.reason());
                    if (firstMatch == null) {
                        firstMatch = token;
                    }
                }
            }
            if (looksLikeTest(relative) && score > 0) {
                score += 2;
                reasons.add("test source");
            }
            if (score == 0) {
                return Candidate.none(relative);
            }
            return new Candidate(relative, score, String.join(", ", reasons),
                    numberedExcerpt(content, firstMatch == null ? "" : firstMatch.value(), 120));
        } catch (IOException e) {
            return Candidate.none(relative);
        }
    }

    private boolean allowed(Path repository, Path path) {
        Path relative = repository.relativize(path);
        for (Path element : relative) {
            if (SKIPPED.contains(element.toString())) {
                return false;
            }
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".java") || name.endsWith(".kt") || name.endsWith(".groovy")
                || name.endsWith(".gradle") || name.endsWith(".kts") || name.endsWith(".xml")
                || name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties");
    }

    private String numberedExcerpt(String content, String token, int maxLines) {
        List<String> lines = content.lines().toList();
        if (lines.size() <= maxLines) {
            return number(lines, 0, lines.size());
        }
        int match = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (containsToken(lines.get(i), token)) {
                match = i;
                break;
            }
        }
        int from = Math.max(0, (Math.max(match, 0)) - 40);
        int to = Math.min(lines.size(), from + maxLines);
        return number(lines, from, to);
    }

    private String number(List<String> lines, int from, int to) {
        StringBuilder result = new StringBuilder();
        for (int i = from; i < to; i++) {
            result.append(String.format("%4d | %s%n", i + 1, lines.get(i)));
        }
        return result.toString();
    }

    private boolean looksLikeTest(String path) {
        String lower = path.toLowerCase();
        return lower.contains("/test/") || lower.endsWith("test.java") || lower.endsWith("test.kt");
    }

    private static boolean containsToken(String haystack, String needle) {
        if (needle == null || needle.isBlank()) {
            return false;
        }
        String lowerHaystack = haystack.toLowerCase();
        String lowerNeedle = needle.toLowerCase();
        if (lowerHaystack.contains(lowerNeedle)) {
            return true;
        }
        return normalize(lowerHaystack).contains(normalize(lowerNeedle));
    }

    private static String normalize(String value) {
        return value.replaceAll("[^a-z0-9]", "");
    }

    private String simpleToken(String value) {
        int dot = value.lastIndexOf('.');
        return dot >= 0 ? value.substring(dot + 1) : value;
    }

    private List<SearchToken> searchTokens(DependencyUpdate update) {
        List<SearchToken> tokens = new ArrayList<>();
        tokens.add(new SearchToken(update.artifact(), 6, "artifact coordinate"));
        tokens.add(new SearchToken(simpleToken(update.artifact()), 5, "artifact token"));
        tokens.add(new SearchToken(update.group(), 4, "group coordinate"));

        if (isBom(update.artifact())) {
            String managedGroup = parentGroup(update.group());
            if (!managedGroup.equals(update.group())) {
                tokens.add(new SearchToken(managedGroup, 4, "BOM-managed group"));
            }
            tokens.add(new SearchToken(simpleToken(update.group()), 2, "BOM group token"));
        } else {
            tokens.add(new SearchToken(simpleToken(update.group()), 3, "group token"));
        }

        Set<String> seen = new LinkedHashSet<>();
        return tokens.stream()
                .filter(token -> seen.add(token.value().toLowerCase()))
                .toList();
    }

    private boolean isBom(String artifact) {
        return artifact.toLowerCase().endsWith("-bom") || artifact.equalsIgnoreCase("bom");
    }

    private String parentGroup(String group) {
        int lastDot = group.lastIndexOf('.');
        return lastDot > 0 ? group.substring(0, lastDot) : group;
    }

    private record Candidate(String relativePath, int score, String reason, String content) {
        static Candidate none(String path) {
            return new Candidate(path, 0, "", "");
        }
    }

    private record SearchToken(String value, int score, String reason) {}
}
