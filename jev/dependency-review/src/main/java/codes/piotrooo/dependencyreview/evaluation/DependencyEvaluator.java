package codes.piotrooo.dependencyreview.evaluation;

import codes.piotrooo.dependencyreview.dependency.DependencyUpdate;
import codes.piotrooo.dependencyreview.investigation.DependencyEvidence;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Choice;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.Score;
import org.springaicommunity.typesafe.response.ChoiceAnswer;
import org.springaicommunity.typesafe.response.ScoreAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DependencyEvaluator {

    private final TypeSafeClient typeSafeClient;

    public DependencyEvaluator(TypeSafeClient typeSafeClient) {
        this.typeSafeClient = typeSafeClient;
    }

    public DependencyEvaluation evaluate(DependencyUpdate update, DependencyEvidence evidence) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("dependency", update.coordinate());
        state.put("previousVersion", update.fromVersion());
        state.put("newVersion", update.toVersion());
        state.put("repositoryEvidence", evidence);

        SystemOneResponse response = typeSafeClient.systemOne(
                state,
                Map.of(
                        "migration_required", Noul.of("""
                                Based on the repository and upstream release-note evidence, does this dependency update likely require
                                changes to the application's existing source code or configuration?
                                """),
                        "runtime_behavior_change", Noul.of("""
                                Based on the repository and upstream release-note evidence, could this dependency update materially affect
                                runtime behavior that this application relies on even if the project still compiles?
                                """),
                        "usage_impact", Score.of(
                                "How important is the observed dependency usage to this application's runtime behavior?",
                                "Not used by production code",
                                "Used in an isolated non-critical path",
                                "Used by multiple application components",
                                "Used in an important application path",
                                "Widely used or infrastructure-critical"),
                        "test_coverage", Score.of(
                                "How well is the observed application usage of this dependency protected by relevant tests?",
                                "No relevant tests found",
                                "Only indirect test coverage",
                                "Partial behavior coverage",
                                "Strong coverage of relevant behavior",
                                "Dedicated compatibility coverage"),
                        "primary_affected_area", Choice.builder()
                                .instructions("Which area is most directly affected by the observed dependency usage?")
                                .option("BUILD", "Build tooling or compile-time integration")
                                .option("API_USAGE", "Direct calls to library APIs")
                                .option("CONFIGURATION", "Properties, beans, or framework configuration")
                                .option("RUNTIME", "Runtime infrastructure or application behavior")
                                .option("SECURITY", "Authentication, authorization, cryptography, or security controls")
                                .option("DATA", "Serialization, persistence, schemas, or data formats")
                                .build()));

        double migrationRequired = response.noulValue("migration_required");
        double runtimeBehaviorChange = response.noulValue("runtime_behavior_change");
        ScoreAnswer usage = response.score("usage_impact");
        ScoreAnswer tests = response.score("test_coverage");
        ChoiceAnswer area = response.choice("primary_affected_area");

        return new DependencyEvaluation(
                migrationRequired,
                runtimeBehaviorChange,
                new DependencyEvaluation.Metric(usage.value(), usage.confidence(), usage.nearestLabel()),
                new DependencyEvaluation.Metric(tests.value(), tests.confidence(), tests.nearestLabel()),
                new DependencyEvaluation.ChoiceMetric(area.value(), area.confidence()));
    }
}
