package codes.piotrooo.dependencyreview.github;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubPullRequestClientTest {

    @Test
    void parsesSshGitHubRemote() {
        var repository = GitHubPullRequestClient.parseRepositoryUrl("git@github.com:thulium/example-service.git");

        assertThat(repository.owner()).isEqualTo("thulium");
        assertThat(repository.name()).isEqualTo("example-service");
    }

    @Test
    void parsesHttpsGitHubRemote() {
        var repository = GitHubPullRequestClient.parseRepositoryUrl("https://github.com/thulium/example-service.git");

        assertThat(repository.owner()).isEqualTo("thulium");
        assertThat(repository.name()).isEqualTo("example-service");
    }
}
