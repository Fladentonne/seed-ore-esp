package dev.seedore;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.BitSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Baut die Vanilla-Erzgenerierung (OreFeature / ScatteredOreFeature + Placement-Modifier) aus dem Seed nach.
 * Nicht nachgebildet: Biom-Filter und der Heightmap-Check am Anfang von OreFeature.place.
 * Deshalb wird beim Rendern zusätzlich gegen den echten Block in der Welt geprüft.
 */
public final class OreSim {
	private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private final ClientLevel level;
	private final int minY;
	private final int topY;
	/** Kalibrier-Modus: alle vorhergesagten Positionen ausgeben, ohne Block-/Luft-Prüfung. */
	private final boolean calib;
	private final BitSet bits = new BitSet();
	/** Positionen, die in diesem Chunk schon von der Simulation belegt wurden (Vanilla: dort ist dann schon Erz -> Ziel passt nicht mehr). */
	private final java.util.HashSet<Long> placed = new java.util.HashSet<>();

	public void resetPlaced() { placed.clear(); }
	private final BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

	public OreSim(ClientLevel level, int minY, int topY, boolean calib) {
		this.level = level;
		this.minY = minY;
		this.topY = topY;
		this.calib = calib;
	}

	/** Originale Vanilla-Berechnung (Xoroshiro, ab 1.18): Population-Seed fuer einen Chunk. */
	public static long populationSeed(long seed, int chunkX, int chunkZ) {
		WorldgenRandom r = new WorldgenRandom(new XoroshiroRandomSource(0L));
		return r.setDecorationSeed(seed, chunkX << 4, chunkZ << 4);
	}

	public void simulate(OreFeatureDef d, int index, long popSeed, int cx, int cz, LongArrayList out) {
		// WorldgenRandom.setFeatureSeed
		WorldgenRandom r = new WorldgenRandom(new XoroshiroRandomSource(0L));
		r.setFeatureSeed(popSeed, index, d.step());
		if (d.rarity() > 0 && !(r.nextFloat() < 1.0F / d.rarity())) return;
		int count = d.countMin() == d.countMax() ? d.countMin()
				: d.countMin() + r.nextInt(d.countMax() - d.countMin() + 1);
		for (int i = 0; i < count; i++) {
			int x = (cx << 4) + r.nextInt(16);
			int z = (cz << 4) + r.nextInt(16);
			int y = sampleY(d, r);
			if (d.scattered()) scattered(d, r, x, y, z, out);
			else vein(d, r, x, y, z, out);
		}
	}

	private int sampleY(OreFeatureDef d, RandomSource r) {
		int lo = d.lo().resolve(minY, topY);
		int hi = d.hi().resolve(minY, topY);
		if (lo > hi) return lo;
		int k = hi - lo;
		if (!d.triangle() || k <= 0) return lo + r.nextInt(hi - lo + 1);
		int l = k / 2;
		int m = k - l;
		return lo + r.nextInt(m + 1) + r.nextInt(l + 1);
	}

	// ---- ScatteredOreFeature (Ancient Debris) ----
	private void scattered(OreFeatureDef d, RandomSource r, int x, int y, int z, LongArrayList out) {
		int n = r.nextInt(d.size() + 1);
		for (int j = 0; j < n; j++) {
			int spread = Math.min(j, 7);
			int dx = Math.round((r.nextFloat() - r.nextFloat()) * spread);
			int dy = Math.round((r.nextFloat() - r.nextFloat()) * Math.min(spread, 2));
			int dz = Math.round((r.nextFloat() - r.nextFloat()) * spread);
			int px = x + dx, py = y + dy, pz = z + dz;
			if (canPlace(d, px, py, pz, r)) out.add(BlockPos.asLong(px, py, pz));
		}
	}

	// ---- OreFeature (Adern) ----
	private void vein(OreFeatureDef d, RandomSource r, int x, int y, int z, LongArrayList out) {
		int size = d.size();
		float f = r.nextFloat() * (float) Math.PI;
		float g = size / 8.0F;
		int i = (int) Math.ceil((size / 16.0F * 2.0F + 1.0F) / 2.0F);
		double x1 = x + Math.sin(f) * g;
		double x2 = x - Math.sin(f) * g;
		double z1 = z + Math.cos(f) * g;
		double z2 = z - Math.cos(f) * g;
		double y1 = y + r.nextInt(3) - 2;
		double y2 = y + r.nextInt(3) - 2;
		int ceilG = (int) Math.ceil(g);
		int n = x - ceilG - i;
		int o = y - 2 - i;
		int p = z - ceilG - i;
		int q = 2 * (ceilG + i);
		int rr = 2 * (2 + i);

		double[] ds = new double[size * 4];
		for (int k = 0; k < size; k++) {
			float t = (float) k / (float) size;
			double e = lerp(t, x1, x2);
			double gy = lerp(t, y1, y2);
			double h = lerp(t, z1, z2);
			double l = r.nextDouble() * size / 16.0;
			double m = ((net.minecraft.util.Mth.sin((float) Math.PI * t) + 1.0F) * l + 1.0) / 2.0;
			ds[k * 4] = e;
			ds[k * 4 + 1] = gy;
			ds[k * 4 + 2] = h;
			ds[k * 4 + 3] = m;
		}
		for (int k = 0; k < size - 1; k++) {
			if (ds[k * 4 + 3] <= 0.0) continue;
			for (int l = k + 1; l < size; l++) {
				if (ds[l * 4 + 3] <= 0.0) continue;
				double dx = ds[k * 4] - ds[l * 4];
				double dy = ds[k * 4 + 1] - ds[l * 4 + 1];
				double dz = ds[k * 4 + 2] - ds[l * 4 + 2];
				double dr = ds[k * 4 + 3] - ds[l * 4 + 3];
				if (dr * dr > dx * dx + dy * dy + dz * dz) {
					if (dr > 0.0) ds[l * 4 + 3] = -1.0;
					else ds[k * 4 + 3] = -1.0;
				}
			}
		}

		bits.clear();
		for (int k = 0; k < size; k++) {
			double e = ds[k * 4 + 3];
			if (e < 0.0) continue;
			double gx = ds[k * 4];
			double gy = ds[k * 4 + 1];
			double gz = ds[k * 4 + 2];
			int u = Math.max(floor(gx - e), n);
			int v = Math.max(floor(gy - e), o);
			int w = Math.max(floor(gz - e), p);
			int aa = Math.max(floor(gx + e), u);
			int ab = Math.max(floor(gy + e), v);
			int ac = Math.max(floor(gz + e), w);
			for (int ad = u; ad <= aa; ad++) {
				double ae = (ad + 0.5 - gx) / e;
				if (ae * ae >= 1.0) continue;
				for (int af = v; af <= ab; af++) {
					double ag = (af + 0.5 - gy) / e;
					if (ae * ae + ag * ag >= 1.0) continue;
					for (int ah = w; ah <= ac; ah++) {
						double ai = (ah + 0.5 - gz) / e;
						if (ae * ae + ag * ag + ai * ai >= 1.0 || level.isOutsideBuildHeight(af)) continue;
						int idx = ad - n + (af - o) * q + (ah - p) * q * rr;
						if (bits.get(idx)) continue;
						bits.set(idx);
						if (canPlace(d, ad, af, ah, r)) out.add(BlockPos.asLong(ad, af, ah));
					}
				}
			}
		}
	}

	private static double lerp(double t, double a, double b) { return a + t * (b - a); }
	private static int floor(double v) { int i = (int) v; return v < i ? i - 1 : i; }

	/** OreFeature.canPlaceOre: Zielblock pruefen, dann Luft-Kontakt (verbraucht wie in Vanilla einen Zufallswert). */
	private boolean canPlace(OreFeatureDef d, int x, int y, int z, RandomSource r) {
		long key = BlockPos.asLong(x, y, z);
		if (placed.contains(key)) return false;
		BlockState s = st(x, y, z);
		if (s != null && !hostOk(d.type(), s)) return false;
		float c = d.discard();
		boolean ok;
		if (c <= 0.0F) ok = true;
		else if (c < 1.0F && r.nextFloat() >= c) ok = true;
		else ok = !exposed(x, y, z);
		if (ok) placed.add(key);
		return ok;
	}

	private boolean exposed(int x, int y, int z) {
		for (int[] dir : DIRS) {
			BlockState n = st(x + dir[0], y + dir[1], z + dir[2]);
			if (n != null && n.isAir()) return true;
		}
		return false;
	}

	/** Blockstate oder null, wenn der Chunk (noch) nicht geladen ist. */
	public BlockState st(int x, int y, int z) {
		LevelChunk c = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
		if (c == null) return null;
		return c.getBlockState(mp.set(x, y, z));
	}

	// ---- Block-Helfer ----
	public static String path(BlockState s) {
		return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
	}

	public static boolean isOre(BlockState s) {
		String p = path(s);
		return p.endsWith("_ore") || p.equals("ancient_debris");
	}

	public static boolean isOfType(OreType t, BlockState s) {
		return t.matches(path(s));
	}

	/** Ist der Block ein Wirtsgestein fuer dieses Erz (oder schon dieses Erz selbst)? Andere Erze blockieren. */
	public static boolean hostOk(OreType t, BlockState s) {
		if (isOfType(t, s)) return true;
		if (t.nether) return s.is(BlockTags.BASE_STONE_NETHER);
		return s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
	}
}
