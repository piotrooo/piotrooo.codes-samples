package codes.piotrooo.dependencyreview.investigation;

import codes.piotrooo.dependencyreview.dependency.DependencyUpdate;
import codes.piotrooo.dependencyreview.git.GitDiffReader.GitDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryContextCollectorTest {

    private final RepositoryContextCollector collector = new RepositoryContextCollector();

    @Test
    void collectsManagedArtifactsAndTheirUsagesForBomUpdates(@TempDir Path repository) throws IOException {
        Files.writeString(repository.resolve("build.gradle"), """
                dependencies {
                    implementation(platform("com.thulium.sdk.domain:thulium-sdk-domain-bom:1.13.1"))
                    implementation("com.thulium.sdk.orders:orders-client")
                }
                """);
        Path source = repository.resolve("src/main/java/example/OrderService.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "import com.thulium.sdk.orders.OrderClient;\nclass OrderService {}\n");
        Path test = repository.resolve("src/test/java/example/OrderServiceTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, "import com.thulium.sdk.orders.OrderClient;\nclass OrderServiceTest {}\n");

        var snippets = collector.collect(
                repository,
                new GitDiff("main", "HEAD", "", List.of("build.gradle")),
                new DependencyUpdate("com.thulium.sdk.domain", "thulium-sdk-domain-bom", "1.12.0", "1.13.1"),
                30);

        assertThat(snippets).extracting(SourceSnippet::path)
                .contains("build.gradle", "src/main/java/example/OrderService.java", "src/test/java/example/OrderServiceTest.java");
        assertThat(snippets).anySatisfy(snippet ->
                assertThat(snippet.reason()).contains("BOM-managed group"));
    }
}
