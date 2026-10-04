package codes.piotrooo.dependencyreview.investigation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EvidenceVerifier {

    public DependencyEvidence verify(DependencyEvidence evidence, List<SourceSnippet> snippets, String releaseNotes) {
        Map<String, SourceSnippet> byPath = snippets.stream()
                .collect(Collectors.toMap(SourceSnippet::path, Function.identity(), (a, b) -> a));

        return new DependencyEvidence(
                evidence.summary(),
                verifyItems(evidence.productionUsages(), byPath),
                verifyItems(evidence.configurationUsages(), byPath),
                verifyItems(evidence.relevantTests(), byPath),
                verifyUpstreamChanges(evidence.upstreamChanges(), releaseNotes),
                evidence.unknowns() == null ? List.of() : evidence.unknowns());
    }

    private List<DependencyEvidence.EvidenceItem> verifyItems(
            List<DependencyEvidence.EvidenceItem> items,
            Map<String, SourceSnippet> snippets) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item.path() != null && snippets.containsKey(item.path()))
                .filter(item -> item.source() != null && !item.source().isBlank())
                .filter(item -> snippets.get(item.path()).content().contains(item.source()))
                .toList();
    }

    private List<DependencyEvidence.UpstreamChange> verifyUpstreamChanges(
            List<DependencyEvidence.UpstreamChange> changes,
            String releaseNotes) {
        if (changes == null || releaseNotes == null || releaseNotes.isBlank()) {
            return List.of();
        }
        return changes.stream()
                .filter(change -> change.source() != null && !change.source().isBlank())
                .filter(change -> releaseNotes.contains(change.source()))
                .toList();
    }
}
