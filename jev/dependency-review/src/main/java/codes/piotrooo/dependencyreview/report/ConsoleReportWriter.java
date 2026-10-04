package codes.piotrooo.dependencyreview.report;

import codes.piotrooo.dependencyreview.dependency.DependencyUpdate;
import codes.piotrooo.dependencyreview.evaluation.DependencyEvaluation;
import codes.piotrooo.dependencyreview.investigation.DependencyEvidence;
import codes.piotrooo.dependencyreview.investigation.SourceSnippet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConsoleReportWriter {

    public void dryRun(DependencyUpdate update, List<SourceSnippet> snippets, String diff) {
        printHeader("🧪 Dependency Review — dry run", update);
        System.out.println("\n🧩 Git diff");
        System.out.println(diff);
        System.out.printf("%n📚 Collected repository snippets (%d)%n", snippets.size());
        for (SourceSnippet snippet : snippets) {
            System.out.printf("%n[%s] %s%n%s%n", snippet.reason(), snippet.path(), snippet.content());
        }
    }

    public void write(
            DependencyUpdate update,
            DependencyEvidence evidence,
            DependencyEvaluation evaluation,
            Timings timings) {
        printHeader("🔎 Dependency Review", update);

        System.out.println("\n📚 Repository evidence");
        printEvidence(evidence);
        printUpstreamChanges(evidence.upstreamChanges());
        if (!evidence.unknowns().isEmpty()) {
            System.out.println("\n❓ Unknowns");
            printTable(List.of("Observation"), evidence.unknowns().stream().map(List::of).toList());
        }

        System.out.println("\n📊 Jev evaluation");
        printTable(
                List.of("Assessment", "Score", "Confidence", "Details"),
                List.of(
                        List.of("Migration required", score(evaluation.migrationRequired()), "—", ""),
                        List.of("Runtime behavior change", score(evaluation.runtimeBehaviorChange()), "—", ""),
                        List.of("Usage impact", score(evaluation.usageImpact().value()) + " / 4", score(evaluation.usageImpact().confidence()), evaluation.usageImpact().label()),
                        List.of("Relevant test coverage", score(evaluation.relevantTestCoverage().value()) + " / 4", score(evaluation.relevantTestCoverage().confidence()), evaluation.relevantTestCoverage().label()),
                        List.of("Primary affected area", evaluation.primaryAffectedArea().value(), score(evaluation.primaryAffectedArea().confidence()), "")));

        System.out.println("\n⏱️  Timing");
        printTable(
                List.of("Stage", "Duration"),
                List.of(
                        List.of("Git + context collection", display(timings.context())),
                        List.of("Spring AI investigation", display(timings.investigation())),
                        List.of("Jev evaluation", display(timings.evaluation())),
                        List.of("Total", display(timings.total()))));
    }

    private void printHeader(String title, DependencyUpdate update) {
        System.out.printf("%n%s%n", title);
        System.out.println("════════════════════════════════════════════════════════════");
        printTable(
                List.of("Dependency", "From", "To"),
                List.of(List.of(update.coordinate(), update.fromVersion(), update.toVersion())));
    }

    private void printUpstreamChanges(List<DependencyEvidence.UpstreamChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        System.out.println("\n🗒️  Upstream release notes");
        printTable(List.of("Change"), changes.stream().map(change -> List.of(change.explanation())).toList());
    }

    private void printEvidence(DependencyEvidence evidence) {
        var rows = new ArrayList<List<String>>();
        addEvidenceRows(rows, "🏭 Production", evidence.productionUsages());
        addEvidenceRows(rows, "⚙️  Configuration", evidence.configurationUsages());
        addEvidenceRows(rows, "🧪 Tests", evidence.relevantTests());

        if (rows.isEmpty()) {
            System.out.println("  — No repository evidence found.");
            return;
        }
        printTable(List.of("Area", "File", "Finding"), rows);
    }

    private void addEvidenceRows(List<List<String>> rows, String area, List<DependencyEvidence.EvidenceItem> items) {
        for (var item : items) {
            rows.add(List.of(area, item.path(), item.explanation()));
        }
    }

    private void printTable(List<String> headers, List<List<String>> rows) {
        var widths = new ArrayList<Integer>();
        for (int column = 0; column < headers.size(); column++) {
            int width = headers.get(column).length();
            for (var row : rows) {
                width = Math.max(width, row.get(column).length());
            }
            widths.add(width);
        }

        printBorder("┌", "┬", "┐", widths);
        printRow(headers, widths);
        printBorder("├", "┼", "┤", widths);
        for (var row : rows) {
            printRow(row, widths);
        }
        printBorder("└", "┴", "┘", widths);
    }

    private void printBorder(String left, String join, String right, List<Integer> widths) {
        var cells = widths.stream().map(width -> "─".repeat(width + 2)).toList();
        System.out.println(left + String.join(join, cells) + right);
    }

    private void printRow(List<String> values, List<Integer> widths) {
        var cells = new ArrayList<String>();
        for (int column = 0; column < values.size(); column++) {
            cells.add(" " + values.get(column) + " ".repeat(widths.get(column) - values.get(column).length() + 1));
        }
        System.out.println("│" + String.join("│", cells) + "│");
    }

    private String score(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String display(Duration duration) {
        return String.format(Locale.ROOT, "%.3f s", duration.toNanos() / 1_000_000_000.0);
    }

    public record Timings(Duration context, Duration investigation, Duration evaluation, Duration total) {}
}
