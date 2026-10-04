package codes.piotrooo.dependencyreview.dependency;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyUpdateDetector {

    private static final Pattern COORDINATE = Pattern.compile(
            "(?<group>[A-Za-z0-9_.-]+):(?<artifact>[A-Za-z0-9_.-]+):(?<version>[A-Za-z0-9_.+\\-]+)");
    private static final Pattern MAVEN_DEPENDENCY_UPDATE = Pattern.compile(
            "(?ms)^\\s+<dependency>\\R"
                    + "^\\s+<groupId>(?<group>[^<\\s]+)</groupId>\\R"
                    + "^\\s+<artifactId>(?<artifact>[^<\\s]+)</artifactId>\\R"
                    + "^-\\s*<version>(?<from>[^<\\s]+)</version>\\R"
                    + "^\\+\\s*<version>(?<to>[^<\\s]+)</version>");

    public DependencyUpdate detectSingle(String diff) {
        var updates = updates(diff);

        if (updates.isEmpty()) {
            throw new IllegalStateException("No explicit group:artifact:version dependency update found in the diff. "
                    + "Use --dependency, --from and --to for version catalogs, BOMs or property-based versions.");
        }
        if (updates.size() > 1) {
            throw new IllegalStateException("Found " + updates.size()
                    + " dependency updates. MVP supports one update at a time. Use --dependency to select one.");
        }
        return updates.getFirst();
    }

    public Optional<DependencyUpdate> detectSelected(String diff, String coordinate) {
        return updates(diff).stream()
                .filter(update -> update.coordinate().equals(coordinate))
                .findFirst();
    }

    private List<DependencyUpdate> updates(String diff) {
        Map<String, String> removed = coordinates(diff, '-');
        Map<String, String> added = coordinates(diff, '+');
        Map<String, DependencyUpdate> updates = new LinkedHashMap<>();
        for (var entry : removed.entrySet()) {
            String coordinate = entry.getKey();
            if (added.containsKey(coordinate) && !entry.getValue().equals(added.get(coordinate))) {
                String[] parts = coordinate.split(":", 2);
                updates.put(coordinate, new DependencyUpdate(parts[0], parts[1], entry.getValue(), added.get(coordinate)));
            }
        }

        Matcher mavenUpdate = MAVEN_DEPENDENCY_UPDATE.matcher(diff);
        while (mavenUpdate.find()) {
            DependencyUpdate update = new DependencyUpdate(
                    mavenUpdate.group("group"),
                    mavenUpdate.group("artifact"),
                    mavenUpdate.group("from"),
                    mavenUpdate.group("to"));
            updates.put(update.coordinate(), update);
        }
        return List.copyOf(updates.values());
    }

    private Map<String, String> coordinates(String diff, char prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        diff.lines()
                .filter(line -> line.length() > 1 && line.charAt(0) == prefix)
                .filter(line -> !line.startsWith("---") && !line.startsWith("+++"))
                .forEach(line -> {
                    Matcher matcher = COORDINATE.matcher(line.substring(1));
                    while (matcher.find()) {
                        String key = matcher.group("group") + ":" + matcher.group("artifact");
                        result.put(key, matcher.group("version"));
                    }
                });
        return result;
    }
}
