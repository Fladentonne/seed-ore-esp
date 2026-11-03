package dev.seedore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config instance = new Config();
	/** Wird bei jeder Änderung erhöht, die die Berechnung ungültig macht (Seed, Indizes). */
	public static int rev = 0;

	public String seed = "";
	public boolean enabled = true;
	public int range = 6;
	public float lineWidth = 2.5f;
	public int maxBoxes = 4000;
	public Map<String, Boolean> typeOn = new HashMap<>();
	public Map<String, Integer> typeColor = new HashMap<>();
	/** Kalibrierte Feature-Indizes (Name -> Index). */
	public Map<String, Integer> index = new HashMap<>();

	public static Config get() { return instance; }

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("seedoresp.json");
	}

	public static void load() {
		try {
			Path f = file();
			if (Files.exists(f)) {
				Config c = GSON.fromJson(Files.readString(f), Config.class);
				if (c != null) instance = c;
			}
		} catch (Exception e) {
			System.err.println("[seedoresp] Config konnte nicht geladen werden: " + e);
		}
		rev++;
	}

	public static void save() {
		try {
			Files.writeString(file(), GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[seedoresp] Config konnte nicht gespeichert werden: " + e);
		}
	}

	public static void touch() { rev++; }

	/** Seed wie im Spiel: Zahl, sonst String-Hash. null = nichts eingegeben. */
	public Long seedValue() {
		String s = seed == null ? "" : seed.trim();
		if (s.isEmpty()) return null;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return (long) s.hashCode();
		}
	}

	public boolean typeEnabled(OreType t) { return typeOn.getOrDefault(t.name(), t.defaultOn); }
	public int color(OreType t) { return typeColor.getOrDefault(t.name(), t.defaultColor); }
	public int indexFor(OreFeatureDef d) { return index.getOrDefault(d.name(), d.defaultIndex()); }
}
