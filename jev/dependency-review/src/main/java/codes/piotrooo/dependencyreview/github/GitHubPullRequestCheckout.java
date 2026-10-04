package codes.piotrooo.dependencyreview.github;

import codes.piotrooo.dependencyreview.git.GitCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** A short-lived local checkout used to inspect a GitHub PR supplied by URL. */
public final class GitHubPullRequestCheckout implements AutoCloseable {

    private static final String BASE_REF = "refs/remotes/origin/dependency-review-base";
    private static final String HEAD_REF = "refs/remotes/origin/dependency-review-head";

    private final Path directory;

    private GitHubPullRequestCheckout(Path directory) {
        this.directory = directory;
    }

    public static GitHubPullRequestCheckout create(GitHubPullRequestClient.RemotePullRequest pullRequest) {
        try {
            Path directory = Files.createTempDirectory("dependency-review-pr-");
            GitCommand git = new GitCommand();
            git.run(directory, "init", "--quiet");
            git.run(directory, "remote", "add", "origin", "https://github.com/%s/%s.git"
                    .formatted(pullRequest.repository().owner(), pullRequest.repository().name()));
            git.run(directory, "fetch", "--quiet", "--depth=1", "origin",
                    pullRequest.baseSha() + ":" + BASE_REF,
                    "refs/pull/%d/head:%s".formatted(pullRequest.number(), HEAD_REF));
            git.run(directory, "checkout", "--quiet", "--detach", HEAD_REF);
            return new GitHubPullRequestCheckout(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create temporary checkout for " + pullRequest.url(), e);
        }
    }

    public Path directory() {
        return directory;
    }

    public String baseRef() {
        return BASE_REF;
    }

    public String headRef() {
        return HEAD_REF;
    }

    @Override
    public void close() {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // The temporary checkout is best-effort cleanup only.
                }
            });
        } catch (IOException ignored) {
            // The temporary checkout is best-effort cleanup only.
        }
    }
}
