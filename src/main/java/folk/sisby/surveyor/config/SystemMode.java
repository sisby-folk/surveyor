package folk.sisby.surveyor.config;

public enum SystemMode implements Comparable<SystemMode> {
    DISABLED(0),
    FROZEN(1),
    DYNAMIC(2),
    ENABLED(3);

    final int level;

    SystemMode(int level) {
        this.level = level;
    }

    public boolean atLeast(SystemMode other) {
        return this.level >= other.level;
    }
}
