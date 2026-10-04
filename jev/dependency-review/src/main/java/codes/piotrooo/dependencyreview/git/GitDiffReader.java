package codes.piotrooo.dependencyreview.git;

import java.nio.file.Path;
import java.util.List;

public class GitDiffReader {

    private final GitCommand git = new GitCommand();

    public GitDiff read(Path repository, String baseRef, String headRef) {
        String range = baseRef + "..." + headRef;
        return readRange(repository, baseRef, headRef, range);
    }

    /** Reads the direct base-to-head comparison when Git history is intentionally shallow. */
    public GitDiff readDirect(Path repository, String baseRef, String headRef) {
        String range = baseRef + ".." + headRef;
        return readRange(repository, baseRef, headRef, range);
    }

    private GitDiff readRange(Path repository, String baseRef, String headRef, String range) {
        String diff = git.run(repository, "diff", "--no-ext-diff", "--unified=20", range);
        List<String> files = git.run(repository, "diff", "--name-only", range).lines()
                .filter(line -> !line.isBlank())
                .toList();
        return new GitDiff(baseRef, headRef, diff, files);
    }

    public record GitDiff(String baseRef, String headRef, String patch, List<String> changedFiles) {}
}
