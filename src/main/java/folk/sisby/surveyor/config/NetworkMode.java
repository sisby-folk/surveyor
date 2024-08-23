package folk.sisby.surveyor.config;

public enum NetworkMode implements Comparable<NetworkMode> {
	NONE(0),
	SOLO(1),
	GROUP(2),
	SERVER(3);

	final int level;

	NetworkMode(int level) {
		this.level = level;
	}

	public boolean atLeast(NetworkMode other) {
		return this.level >= other.level;
	}

	public boolean atMost(NetworkMode other) {
		return this.level <= other.level;
	}
}
