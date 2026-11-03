package dev.seedore;

import static dev.seedore.OreFeatureDef.Anchor.abs;
import static dev.seedore.OreFeatureDef.Anchor.bottom;
import static dev.seedore.OreFeatureDef.Anchor.top;
import static dev.seedore.OreType.*;

import dev.seedore.OreFeatureDef.Anchor;
import java.util.List;

/**
 * Erz-Features (aus dem Gedächtnis nach dem Stand der 1.21.x-Weltgenerierung).
 * Die defaultIndex-Werte sind Schätzungen - im Menü "Kalibrieren" findet den echten Index selbst.
 */
public final class OreDefs {
	private OreDefs() {}

	private static OreFeatureDef d(OreType t, String n, int step, int idx, int cMin, int cMax, int rarity,
			boolean tri, Anchor lo, Anchor hi, int size, float disc, boolean scattered) {
		return new OreFeatureDef(t, n, step, idx, cMin, cMax, rarity, tri, lo, hi, size, disc, scattered);
	}

	public static final List<OreFeatureDef> ALL = List.of(
		// Overworld, Step 6
		d(COAL, "coal_upper", 6, 9, 30, 30, 0, false, abs(136), abs(320), 17, 0.0f, false),
		d(COAL, "coal_lower", 6, 10, 20, 20, 0, true, abs(0), abs(192), 17, 0.5f, false),
		d(IRON, "iron_upper", 6, 11, 90, 90, 0, true, abs(80), abs(384), 9, 0.0f, false),
		d(IRON, "iron_middle", 6, 12, 10, 10, 0, true, abs(-24), abs(56), 9, 0.0f, false),
		d(IRON, "iron_small", 6, 13, 10, 10, 0, false, bottom(0), abs(72), 4, 0.0f, false),
		d(GOLD, "gold", 6, 15, 4, 4, 0, true, abs(-64), abs(32), 9, 0.5f, false),
		d(GOLD, "gold_lower", 6, 16, 0, 1, 0, false, abs(-64), abs(-48), 9, 0.5f, false),
		d(REDSTONE, "redstone", 6, 17, 4, 4, 0, false, bottom(0), abs(15), 8, 0.0f, false),
		d(REDSTONE, "redstone_lower", 6, 18, 8, 8, 0, true, bottom(-32), bottom(32), 8, 0.0f, false),
		d(DIAMOND, "diamond", 6, 19, 7, 7, 0, true, bottom(-80), bottom(80), 4, 0.5f, false),
		d(DIAMOND, "diamond_medium", 6, 20, 2, 4, 0, false, abs(-64), abs(-4), 8, 0.5f, false),
		d(DIAMOND, "diamond_large", 6, 21, 1, 1, 9, true, bottom(-80), bottom(80), 12, 0.7f, false),
		d(DIAMOND, "diamond_buried", 6, 22, 4, 4, 0, true, bottom(-80), bottom(80), 8, 1.0f, false),
		d(LAPIS, "lapis", 6, 23, 2, 2, 0, true, bottom(-32), bottom(32), 7, 1.0f, false),
		d(LAPIS, "lapis_buried", 6, 24, 4, 4, 0, false, bottom(0), abs(64), 7, 1.0f, false),
		d(COPPER, "copper", 6, 25, 16, 16, 0, true, abs(-16), abs(112), 10, 0.0f, false),
		// Nether, Step 7 (Streu-Erz)
		d(DEBRIS, "debris_large", 7, 1, 1, 1, 0, true, bottom(8), abs(24), 3, 1.0f, true),
		d(DEBRIS, "debris_small", 7, 2, 1, 1, 0, false, bottom(8), top(8), 2, 1.0f, true)
	);
}
