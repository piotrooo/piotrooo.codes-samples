package codes.piotrooo.dependencyreview.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import codes.piotrooo.dependencyreview.git.GitCommand;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fetches the body of the GitHub PR associated with the local HEAD commit. */
public class GitHubPullRequestClient {

    private static final Pattern REMOTE = Pattern.compile(
            "(?:git@github\\.com:|https?://github\\.com/)([^/]+)/([^/]+?)(?:\\.git)?/?$");
    private static final Pattern PR_URL = Pattern.compile(
            "https?://github\\.com/([^/]+)/([^/]+)/pull/(\\d+)/?");
    private static final int MAX_BODY_CHARS = 50_000;

    private final GitCommand git;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public GitHubPullRequestClient() {
        this(new GitCommand(), HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), new ObjectMapper());
    }

    GitHubPullRequestClient(GitCommand git, HttpClient http, ObjectMapper mapper) {
        this.git = git;
        this.http = http;
        this.mapper = mapper;
    }

    public Optional<PullRequestNotes> fetch(Path repository, String requestedPr) {
        try {
            Repository remote = repository(repository);
            PullRequestId id = requestedPr == null ? findForHead(repository, remote) : parseRequestedPr(requestedPr, remote);
            return Optional.of(fetchPullRequest(id).notes());
        } catch (Exception e) {
            System.err.println("GitHub PR notes were not loaded: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Loads the repository and refs needed to inspect a remote PR without a local checkout. */
    public RemotePullRequest fetchRemotePullRequest(String requestedPr) {
        if (requestedPr == null || requestedPr.isBlank()) {
            throw new IllegalArgumentException("--github-pr URL is required for remote PR analysis");
        }
        Matcher url = PR_URL.matcher(requestedPr);
        if (!url.matches()) {
            throw new IllegalArgumentException("Remote PR analysis requires a full GitHub PR URL");
        }
        try {
            return fetchPullRequest(new PullRequestId(
                    new Repository(url.group(1), url.group(2)), Integer.parseInt(url.group(3)))).remote();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load GitHub PR " + requestedPr, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub PR request interrupted", e);
        }
    }

    private Repository repository(Path repository) {
        return parseRepositoryUrl(git.run(repository, "remote", "get-url", "origin").trim());
    }

    private PullRequestId findForHead(Path repository, Repository remote) throws IOException, InterruptedException {
        String sha = git.run(repository, "rev-parse", "HEAD").trim();
        JsonNode pulls = get("/repos/%s/%s/commits/%s/pulls".formatted(remote.owner(), remote.name(), sha));
        if (!pulls.isArray() || pulls.isEmpty()) {
            throw new IllegalStateException("no GitHub PR is associated with HEAD " + sha);
        }
        JsonNode pull = null;
        for (JsonNode candidate : pulls) {
            if ("open".equals(candidate.path("state").asText())) {
                pull = candidate;
                break;
            }
        }
        if (pull == null) {
            pull = pulls.get(0);
        }
        return new PullRequestId(remote, pull.path("number").asInt());
    }

    private PullRequestId parseRequestedPr(String requestedPr, Repository defaultRepository) {
        Matcher url = PR_URL.matcher(requestedPr);
        if (url.matches()) {
            return new PullRequestId(new Repository(url.group(1), url.group(2)), Integer.parseInt(url.group(3)));
        }
        try {
            return new PullRequestId(defaultRepository, Integer.parseInt(requestedPr));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--github-pr must be a PR number or https://github.com/owner/repo/pull/number");
        }
    }

    private PullRequestData fetchPullRequest(PullRequestId id) throws IOException, InterruptedException {
        JsonNode pull = get("/repos/%s/%s/pulls/%d".formatted(id.repository().owner(), id.repository().name(), id.number()));
        String body = pull.path("body").asText("").trim();
        if (body.length() > MAX_BODY_CHARS) {
            body = body.substring(0, MAX_BODY_CHARS) + "\n\n[PR description truncated]";
        }
        PullRequestNotes notes = new PullRequestNotes(
                pull.path("html_url").asText(), pull.path("title").asText(), body);
        return new PullRequestData(
                notes,
                new RemotePullRequest(
                        notes.url(),
                        notes.title(),
                        notes.body(),
                        id.repository(),
                        id.number(),
                        pull.path("base").path("ref").asText(),
                        pull.path("base").path("sha").asText(),
                        pull.path("head").path("sha").asText()));
    }

    private JsonNode get(String path) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("https://api.github.com" + path))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2026-03-10")
                .timeout(Duration.ofSeconds(20))
                .GET();
        String token = Optional.ofNullable(System.getenv("GITHUB_TOKEN"))
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(System.getenv("GH_TOKEN")).filter(value -> !value.isBlank()))
                .orElse(null);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GitHub API returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    static Repository parseRepositoryUrl(String remote) {
        Matcher matcher = REMOTE.matcher(remote);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("origin is not a github.com remote: " + remote);
        }
        return new Repository(matcher.group(1), matcher.group(2));
    }

    record Repository(String owner, String name) {}

    private record PullRequestId(Repository repository, int number) {}

    public record PullRequestNotes(String url, String title, String body) {}

    public record RemotePullRequest(
            String url,
            String title,
            String body,
            Repository repository,
            int number,
            String baseRef,
            String baseSha,
            String headSha) {}

    private record PullRequestData(PullRequestNotes notes, RemotePullRequest remote) {}
}
