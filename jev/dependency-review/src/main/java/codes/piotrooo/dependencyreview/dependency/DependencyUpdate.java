package codes.piotrooo.dependencyreview.dependency;

public record DependencyUpdate(
        String group,
        String artifact,
        String fromVersion,
        String toVersion) {

    public String coordinate() {
        return group + ":" + artifact;
    }

    public static DependencyUpdate manual(String coordinate, String from, String to) {
        String[] parts = coordinate.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Dependency must have group:artifact format: " + coordinate);
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Manual dependency override requires --from and --to");
        }
        return new DependencyUpdate(parts[0], parts[1], from, to);
    }
}
