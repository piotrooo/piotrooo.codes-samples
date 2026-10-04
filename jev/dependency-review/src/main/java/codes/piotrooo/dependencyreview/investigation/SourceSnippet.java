package codes.piotrooo.dependencyreview.investigation;

public record SourceSnippet(
        String path,
        String reason,
        String content) {}
