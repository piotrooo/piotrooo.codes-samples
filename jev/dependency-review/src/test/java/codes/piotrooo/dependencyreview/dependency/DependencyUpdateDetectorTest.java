package codes.piotrooo.dependencyreview.dependency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyUpdateDetectorTest {

    private final DependencyUpdateDetector detector = new DependencyUpdateDetector();

    @Test
    void shouldDetectGradleDependencyUpdate() {
        String diff = """
                - implementation("org.example:http-client:2.4.1")
                + implementation("org.example:http-client:3.0.0")
                """;

        var update = detector.detectSingle(diff);

        assertThat(update.coordinate()).isEqualTo("org.example:http-client");
        assertThat(update.fromVersion()).isEqualTo("2.4.1");
        assertThat(update.toVersion()).isEqualTo("3.0.0");
    }

    @Test
    void shouldDetectMavenDependencyUpdateWhenCoordinatesAreDiffContext() {
        String diff = """
                 <dependency>
                     <groupId>com.google.guava</groupId>
                     <artifactId>guava</artifactId>
                -    <version>33.5.0-jre</version>
                +    <version>33.6.0-jre</version>
                 </dependency>
                """;

        var update = detector.detectSingle(diff);

        assertThat(update.coordinate()).isEqualTo("com.google.guava:guava");
        assertThat(update.fromVersion()).isEqualTo("33.5.0-jre");
        assertThat(update.toVersion()).isEqualTo("33.6.0-jre");
    }
}
