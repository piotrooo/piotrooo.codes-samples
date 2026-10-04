package codes.piotrooo.dependencyreview.cli;

import codes.piotrooo.dependencyreview.dependency.DependencyUpdate;
import codes.piotrooo.dependencyreview.dependency.DependencyUpdateDetector;
import codes.piotrooo.dependencyreview.evaluation.DependencyEvaluation;
import codes.piotrooo.dependencyreview.evaluation.DependencyEvaluator;
import codes.piotrooo.dependencyreview.git.GitDiffReader;
import codes.piotrooo.dependencyreview.git.GitDiffReader.GitDiff;
import codes.piotrooo.dependencyreview.github.GitHubPullRequestClient;
import codes.piotrooo.dependencyreview.github.GitHubPullRequestCheckout;
import codes.piotrooo.dependencyreview.investigation.DependencyEvidence;
import codes.piotrooo.dependencyreview.investigation.DependencyInvestigator;
import codes.piotrooo.dependencyreview.investigation.RepositoryContextCollector;
import codes.piotrooo.dependencyreview.investigation.SourceSnippet;
import codes.piotrooo.dependencyreview.report.ConsoleReportWriter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ReviewRunner {

    private final DependencyInvestigator investigator;
    private final DependencyEvaluator evaluator;
    private final GitDiffReader gitDiffReader = new GitDiffReader();
    private final DependencyUpdateDetector updateDetector = new DependencyUpdateDetector();
    private final RepositoryContextCollector contextCollector = new RepositoryContextCollector();
    private final GitHubPullRequestClient gitHubPullRequestClient = new GitHubPullRequestClient();
    private final ConsoleReportWriter reportWriter = new ConsoleReportWriter();

    public ReviewRunner(DependencyInvestigator investigator, DependencyEvaluator evaluator) {
        this.investigator = investigator;
        this.evaluator = evaluator;
    }

    public void run(ApplicationArguments arguments) {
        AnalyzeOptions options;
        try {
            options = AnalyzeOptions.parse(arguments.getSourceArgs());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        Instant totalStart = Instant.now();
        Instant contextStart = Instant.now();

        if (options.githubPr() != null) {
            runRemotePullRequest(options, totalStart, contextStart);
            return;
        }
        runLocalRepository(options, totalStart, contextStart);
    }

    private void runRemotePullRequest(AnalyzeOptions options, Instant totalStart, Instant contextStart) {
        var pullRequest = gitHubPullRequestClient.fetchRemotePullRequest(options.githubPr());
        try (var checkout = GitHubPullRequestCheckout.create(pullRequest)) {
            GitDiff diff = gitDiffReader.readDirect(checkout.directory(), checkout.baseRef(), checkout.headRef());
            runReview(options, checkout.directory(), diff, pullRequest.url(), pullRequest.title(), pullRequest.body(), totalStart, contextStart);
        }
    }

    private void runLocalRepository(AnalyzeOptions options, Instant totalStart, Instant contextStart) {
        GitDiff diff = gitDiffReader.read(options.repository(), options.baseRef(), options.headRef());
        String releaseNotes = readReleaseNotes(options);
        runReview(options, options.repository(), diff, null, null, releaseNotes, totalStart, contextStart);
    }

    private void runReview(
            AnalyzeOptions options,
            java.nio.file.Path repository,
            GitDiff diff,
            String pullRequestUrl,
            String pullRequestTitle,
            String releaseNotes,
            Instant totalStart,
            Instant contextStart) {
        DependencyUpdate update = resolveUpdate(options, diff.patch());
        List<SourceSnippet> snippets = contextCollector.collect(repository, diff, update, options.maxFiles());
        Duration contextTime = Duration.between(contextStart, Instant.now());

        if (options.dryRun()) {
            reportWriter.dryRun(update, snippets, diff.patch());
            return;
        }

        if (pullRequestUrl != null) {
            releaseNotes = "GitHub PR: %s%nTitle: %s%n%n%s".formatted(pullRequestUrl, pullRequestTitle, releaseNotes);
        }
        Instant investigationStart = Instant.now();
        DependencyEvidence evidence = investigator.investigate(update, diff, snippets, releaseNotes);
        Duration investigationTime = Duration.between(investigationStart, Instant.now());

        Instant evaluationStart = Instant.now();
        DependencyEvaluation evaluation = evaluator.evaluate(update, evidence);
        Duration evaluationTime = Duration.between(evaluationStart, Instant.now());

        reportWriter.write(update, evidence, evaluation, new ConsoleReportWriter.Timings(
                contextTime, investigationTime, evaluationTime, Duration.between(totalStart, Instant.now())));
    }

    private DependencyUpdate resolveUpdate(AnalyzeOptions options, String diff) {
        if (options.dependency() == null) {
            return updateDetector.detectSingle(diff);
        }
        if (options.fromVersion() != null || options.toVersion() != null) {
            return DependencyUpdate.manual(options.dependency(), options.fromVersion(), options.toVersion());
        }
        return updateDetector.detectSelected(diff, options.dependency())
                .orElseThrow(() -> new IllegalStateException(
                        "Could not detect versions for %s. Provide --from and --to.".formatted(options.dependency())));
    }

    private String readReleaseNotes(AnalyzeOptions options) {
        if (options.releaseNotesFile() != null) {
            try {
                return Files.readString(options.releaseNotesFile());
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read release notes file %s".formatted(options.releaseNotesFile()), e);
            }
        }
        return gitHubPullRequestClient.fetch(options.repository(), options.githubPr())
                .map(notes -> "GitHub PR: %s%nTitle: %s%n%n%s".formatted(notes.url(), notes.title(), notes.body()))
                .orElse("");
    }
}
