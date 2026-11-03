package dev.seedore;

/** Erzgruppen, die im Menü einzeln an-/ausgeschaltet und eingefärbt werden können. */
public enum OreType {
	COAL("Kohle", "coal", 0xFFAAAAAA, false, false),
	IRON("Eisen", "iron", 0xFFFFA070, false, true),
	COPPER("Kupfer", "copper", 0xFFE77C56, false, false),
	GOLD("Gold", "gold", 0xFFFFD700, false, true),
	REDSTONE("Redstone", "redstone", 0xFFFF0000, false, false),
	LAPIS("Lapis", "lapis", 0xFF3366FF, false, false),
	DIAMOND("Diamant", "diamond", 0xFF00FFFF, false, true),
	DEBRIS("Ancient Debris", "ancient_debris", 0xFFFF00FF, true, true);

	public final String label;
	public final String key;
	public final int defaultColor;
	public final boolean nether;
	public final boolean defaultOn;

	OreType(String label, String key, int defaultColor, boolean nether, boolean defaultOn) {
		this.label = label;
		this.key = key;
		this.defaultColor = defaultColor;
		this.nether = nether;
		this.defaultOn = defaultOn;
	}

	/** Prüft anhand des Block-Pfads (z.B. "deepslate_diamond_ore"), ob der Block zu dieser Gruppe gehört. */
	public boolean matches(String path) {
		if (this == DEBRIS) return path.equals("ancient_debris");
		return path.equals(key + "_ore") || path.equals("deepslate_" + key + "_ore");
	}
}
