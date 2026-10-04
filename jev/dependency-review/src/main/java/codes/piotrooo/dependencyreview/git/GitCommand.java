package codes.piotrooo.dependencyreview.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitCommand {

    public String run(Path repository, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(repository.toFile())
                .redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Git command failed (" + exitCode + "): "
                        + String.join(" ", command) + "\n" + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot execute git. Is it installed?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git command interrupted", e);
        }
    }
}
