package codes.piotrooo.dependencyreview.cli;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AnalyzeOptions(
        Path repository,
        String baseRef,
        String headRef,
        String dependency,
        String fromVersion,
        String toVersion,
        int maxFiles,
        Path releaseNotesFile,
        String githubPr,
        boolean dryRun) {

    public static AnalyzeOptions parse(String[] sourceArgs) {
        List<String> args = List.of(sourceArgs);
        if (args.isEmpty() || !"analyze".equals(args.getFirst())) {
            throw new IllegalArgumentException(usage());
        }

        Map<String, String> values = new HashMap<>();
        boolean dryRun = false;

        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);
            if ("--dry-run".equals(arg)) {
                dryRun = true;
                continue;
            }
            if (!arg.startsWith("--") || i + 1 >= args.size()) {
                throw new IllegalArgumentException("Invalid argument: " + arg + "\n\n" + usage());
            }
            values.put(arg.substring(2), args.get(++i));
        }

        String dependency = values.get("dependency");
        String from = values.get("from");
        String to = values.get("to");
        if ((from != null || to != null) && dependency == null) {
            throw new IllegalArgumentException("--from/--to require --dependency");
        }

        return new AnalyzeOptions(
                Path.of(values.getOrDefault("repo", ".")).toAbsolutePath().normalize(),
                values.getOrDefault("base", "main"),
                values.getOrDefault("head", "HEAD"),
                dependency,
                from,
                to,
                Integer.parseInt(values.getOrDefault("max-files", "30")),
                optionalPath(values.get("release-notes-file")),
                values.get("github-pr"),
                dryRun);
    }

    private static Path optionalPath(String value) {
        return value == null ? null : Path.of(value).toAbsolutePath().normalize();
    }

    public static String usage() {
        return """
                Usage:
                  dependency-review analyze [options]

                Options:
                  --repo <path>          Repository to analyze (default: .)
                  --base <ref>           Base Git ref (default: main)
                  --head <ref>           Head Git ref (default: HEAD)
                  --dependency <g:a>     Manual dependency coordinate override
                  --from <version>       Manual previous version
                  --to <version>         Manual new version
                  --max-files <n>        Max repository snippets to collect (default: 30)
                  --release-notes-file <path>
                                         Markdown or text release notes to include as upstream evidence
                  --github-pr <url>     Analyze this GitHub PR remotely (including its diff and source)
                  --dry-run              Stop after deterministic context collection
                """;
    }
}
