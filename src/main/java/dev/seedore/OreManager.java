package dev.seedore;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Hält die berechneten Erz-Positionen pro Chunk und zeichnet sie. */
public final class OreManager {
	private OreManager() {}

	private static final Map<Long, EnumMap<OreType, LongArrayList>> CACHE = new HashMap<>();
	private static List<int[]> offsets = new ArrayList<>();
	private static int offsetsRange = -1;
	private static ClientLevel lastLevel;
	private static String lastKey = "";
	private static int ticks;
	private static volatile boolean calibrating;
	private static int drawnLast;

	private static long key(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

	private static boolean isNether(ClientLevel lv) { return lv.dimension() == Level.NETHER; }
	private static boolean isOverworld(ClientLevel lv) { return lv.dimension() == Level.OVERWORLD; }
	private static int topY(ClientLevel lv) { return lv.getMinY() + (isNether(lv) ? 128 : 384) - 1; }

	private static void ensureOffsets(int range) {
		if (range == offsetsRange) return;
		offsetsRange = range;
		offsets = new ArrayList<>();
		for (int dx = -range; dx <= range; dx++)
			for (int dz = -range; dz <= range; dz++)
				if (dx * dx + dz * dz <= (range + 1) * (range + 1)) offsets.add(new int[] {dx, dz});
		offsets.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
	}

	private static boolean loaded(ClientLevel lv, int cx, int cz) {
		return lv.getChunkSource().getChunkNow(cx, cz) != null;
	}

	private static boolean neighborhoodLoaded(ClientLevel lv, int cx, int cz) {
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				if (!loaded(lv, cx + x, cz + z)) return false;
		return true;
	}

	public static void tick(Minecraft mc) {
		ClientLevel lv = mc.level;
		LocalPlayer p = mc.player;
		if (lv == null || p == null) {
			CACHE.clear();
			lastLevel = null;
			return;
		}
		Config cfg = Config.get();
		Long seed = cfg.seedValue();
		String k = seed + "|" + lv.dimension() + "|" + Config.rev;
		if (lv != lastLevel || !k.equals(lastKey)) {
			CACHE.clear();
			lastLevel = lv;
			lastKey = k;
		}
		if (!cfg.enabled || seed == null || calibrating) return;
		boolean nether = isNether(lv);
		if (!nether && !isOverworld(lv)) return;

		ensureOffsets(cfg.range);
		int pcx = p.blockPosition().getX() >> 4;
		int pcz = p.blockPosition().getZ() >> 4;
		int budget = 2;
		for (int[] o : offsets) {
			if (budget == 0) break;
			int cx = pcx + o[0], cz = pcz + o[1];
			if (CACHE.containsKey(key(cx, cz)) || !neighborhoodLoaded(lv, cx, cz)) continue;
			compute(lv, seed, nether, cx, cz);
			budget--;
		}
		if (++ticks % 100 == 0) {
			int far = cfg.range + 4;
			CACHE.keySet().removeIf(kk -> {
				int cx = (int) (kk >> 32), cz = (int) (long) kk;
				return Math.abs(cx - pcx) > far || Math.abs(cz - pcz) > far;
			});
		}
	}

	private static void compute(ClientLevel lv, long seed, boolean nether, int cx, int cz) {
		Config cfg = Config.get();
		OreSim sim = new OreSim(lv, lv.getMinY(), topY(lv), false);
		long pop = OreSim.populationSeed(seed, cx, cz);
		EnumMap<OreType, LongArrayList> res = new EnumMap<>(OreType.class);
		for (OreFeatureDef d : OreDefs.ALL) {
			if (d.type().nether != nether) continue;
			LongArrayList list = res.computeIfAbsent(d.type(), t -> new LongArrayList());
			sim.simulate(d, cfg.indexFor(d), pop, cx, cz, list);
		}
		CACHE.put(key(cx, cz), res);
	}

	/** Wird jeden Frame vor den Gizmos aufgerufen. */
	public static void render(Minecraft mc) {
		ClientLevel lv = mc.level;
		LocalPlayer pl = mc.player;
		Config cfg = Config.get();
		if (!cfg.enabled || lv == null || pl == null || CACHE.isEmpty()) return;

		int pcx = pl.blockPosition().getX() >> 4;
		int pcz = pl.blockPosition().getZ() >> 4;
		double px = pl.getX(), py = pl.getY(), pz = pl.getZ();
		double r = cfg.range * 16.0;
		double r2 = r * r;
		BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
		int drawn = 0;
		drawnLast = 0;

		for (int[] o : offsets) {
			EnumMap<OreType, LongArrayList> co = CACHE.get(key(pcx + o[0], pcz + o[1]));
			if (co == null) continue;
			for (Map.Entry<OreType, LongArrayList> e : co.entrySet()) {
				OreType t = e.getKey();
				if (!cfg.typeEnabled(t)) continue;
				GizmoStyle style = GizmoStyle.stroke(cfg.color(t), cfg.lineWidth);
				LongArrayList list = e.getValue();
				for (int i = 0; i < list.size(); i++) {
					long pos = list.getLong(i);
					int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
					double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
					if (dx * dx + dy * dy + dz * dz > r2) continue;
					BlockState s = lv.getBlockState(mp.set(x, y, z));
					// abgebaut / ersetzt -> nicht mehr anzeigen
					if (s.isAir() || !(OreSim.hostOk(t, s) || OreSim.isOre(s))) continue;
					Gizmos.cuboid(new AABB(x, y, z, x + 1, y + 1, z + 1), style).setAlwaysOnTop();
					drawnLast = ++drawn;
					if (drawn >= cfg.maxBoxes) return;
				}
			}
		}
	}

	/** Kurzer Statusbericht fuer den Chat (Taste P). */
	public static String stats() {
		int[] per = new int[OreType.values().length];
		for (EnumMap<OreType, LongArrayList> m : CACHE.values())
			for (Map.Entry<OreType, LongArrayList> e : m.entrySet()) per[e.getKey().ordinal()] += e.getValue().size();
		StringBuilder sb = new StringBuilder(CACHE.size() + " Chunks berechnet, ");
		for (OreType t : OreType.values()) if (per[t.ordinal()] > 0) sb.append(t.label).append(' ').append(per[t.ordinal()]).append(", ");
		sb.append(drawnLast).append(" Boxen gezeichnet");
		return sb.toString();
	}

	// ---- Kalibrierung: findet pro Feature den Index, der am besten zu den echten Erzen passt ----
	public static void chat(Minecraft mc, String msg) {
		mc.execute(() -> { if (mc.player != null) mc.player.sendSystemMessage(Component.literal("[Seed Ore ESP] " + msg)); });
	}

	public static void calibrate(Minecraft mc) {
		ClientLevel lv = mc.level;
		LocalPlayer p = mc.player;
		Long seed = Config.get().seedValue();
		if (lv == null || p == null) return;
		if (seed == null) { chat(mc, "Bitte zuerst einen Seed eingeben."); return; }
		if (calibrating) { chat(mc, "Kalibrierung läuft schon."); return; }
		boolean nether = isNether(lv);
		if (!nether && !isOverworld(lv)) { chat(mc, "In dieser Dimension gibt es nichts zu kalibrieren."); return; }

		int pcx = p.blockPosition().getX() >> 4, pcz = p.blockPosition().getZ() >> 4;
		List<int[]> chunks = new ArrayList<>();
		for (int dx = -3; dx <= 3; dx++)
			for (int dz = -3; dz <= 3; dz++)
				if (loaded(lv, pcx + dx, pcz + dz)) chunks.add(new int[] {pcx + dx, pcz + dz});
		if (chunks.size() < 9) { chat(mc, "Zu wenige geladene Chunks."); return; }

		final long sd = seed;
		final int minY = lv.getMinY(), top = topY(lv);
		calibrating = true;
		chat(mc, "Kalibrierung gestartet (" + chunks.size() + " Chunks) - das kann etwas dauern ...");
		Thread t = new Thread(() -> {
			try {
				OreSim sim = new OreSim(lv, minY, top, true);
				int okCount = 0;
				long[] pops = new long[chunks.size()];
				for (int i = 0; i < pops.length; i++) pops[i] = OreSim.populationSeed(sd, chunks.get(i)[0], chunks.get(i)[1]);
				for (OreFeatureDef d : OreDefs.ALL) {
					if (d.type().nether != nether) continue;
					int best = -1, bestHits = 0, second = 0, bestPred = 0;
					for (int idx = 0; idx < 64; idx++) {
						int hits = 0, pred = 0;
						for (int c = 0; c < pops.length; c++) {
							LongArrayList out = new LongArrayList();
							sim.simulate(d, idx, pops[c], chunks.get(c)[0], chunks.get(c)[1], out);
							for (int i = 0; i < out.size(); i++) {
								long pos = out.getLong(i);
								pred++;
								BlockState s = sim.st(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos));
								if (s != null && OreSim.isOfType(d.type(), s)) hits++;
							}
						}
						if (hits > bestHits) { second = bestHits; bestHits = hits; best = idx; bestPred = pred; }
						else if (hits > second) second = hits;
					}
					// echter Treffer: klar besser als alle anderen Indizes (Zufallstreffer liegen alle auf aehnlichem Niveau)
					boolean ok = best >= 0 && bestHits >= 10 && bestHits >= 3 * Math.max(1, second) && bestHits * 10 >= bestPred;
					final int fb = best, fh = bestHits, fp = bestPred, fs = second;
					if (ok) {
						mc.execute(() -> Config.get().index.put(d.name(), fb));
						okCount++;
						chat(mc, d.name() + ": Index " + fb + " gefunden (" + fh + " von " + fp + " Positionen sind echte Erze)");
					} else {
						chat(mc, d.name() + ": NICHT gefunden (bester Index " + fb + ": " + fh + "/" + fp + ", zweitbester " + fs + ")");
					}
				}
				mc.execute(() -> { Config.touch(); Config.save(); });
				chat(mc, "Kalibrierung fertig. Erfolgreich: " + okCount + " Features. (0 = Seed falsch oder Version weicht ab)");
			} catch (Throwable ex) {
				chat(mc, "Kalibrierung fehlgeschlagen: " + ex);
			} finally {
				calibrating = false;
			}
		}, "seedoresp-calibrate");
		t.setDaemon(true);
		t.start();
	}
}
