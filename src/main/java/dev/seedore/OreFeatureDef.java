package dev.seedore;

/**
 * Beschreibung eines Erz-Features aus der Weltgenerierung (Placement-Modifier + Größe).
 *
 * @param step         GenerationStep.Decoration (6 = UNDERGROUND_ORES, 7 = UNDERGROUND_DECORATION)
 * @param defaultIndex Index des Features in der globalen Feature-Liste dieses Steps (per Kalibrierung überschreibbar)
 * @param countMin     Anzahl Versuche pro Chunk (min)
 * @param countMax     Anzahl Versuche pro Chunk (max, gleich min = konstant)
 * @param rarity       >0: Feature nur mit Chance 1/rarity (RarityFilter)
 * @param triangle     true = Trapez-/Dreiecksverteilung, false = gleichverteilt
 */
public record OreFeatureDef(OreType type, String name, int step, int defaultIndex,
		int countMin, int countMax, int rarity, boolean triangle,
		Anchor lo, Anchor hi, int size, float discard, boolean scattered) {

	/** Höhenanker wie in Vanilla: absolut, über Weltboden oder unter Weltdecke. */
	public record Anchor(int kind, int v) {
		public static Anchor abs(int v) { return new Anchor(0, v); }
		public static Anchor bottom(int v) { return new Anchor(1, v); }
		public static Anchor top(int v) { return new Anchor(2, v); }

		public int resolve(int minY, int topY) {
			return switch (kind) {
				case 0 -> v;
				case 1 -> minY + v;
				default -> topY - v;
			};
		}
	}
}
