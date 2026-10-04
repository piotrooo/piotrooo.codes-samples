package codes.piotrooo.dependencyreview.evaluation;

public record DependencyEvaluation(
        double migrationRequired,
        double runtimeBehaviorChange,
        Metric usageImpact,
        Metric relevantTestCoverage,
        ChoiceMetric primaryAffectedArea) {

    public record Metric(double value, double confidence, String label) {}

    public record ChoiceMetric(String value, double confidence) {}
}
