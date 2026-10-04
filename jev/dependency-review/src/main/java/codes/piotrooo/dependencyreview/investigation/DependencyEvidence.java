package codes.piotrooo.dependencyreview.investigation;

import java.util.List;

public record DependencyEvidence(
        String summary,
        List<EvidenceItem> productionUsages,
        List<EvidenceItem> configurationUsages,
        List<EvidenceItem> relevantTests,
        List<UpstreamChange> upstreamChanges,
        List<String> unknowns) {

    public record EvidenceItem(
            String path,
            String source,
            String explanation) {}

    public record UpstreamChange(
            String source,
            String explanation) {}
}
