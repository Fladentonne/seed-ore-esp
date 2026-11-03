package dev.seedore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menü: Seed, ESP an/aus, Sichtweite, pro Erz an/aus + Farbe, Kalibrierung. */
public class ConfigScreen extends Screen {
	private static final int[] PALETTE = {
		0xFFFF0000, 0xFF00FFFF, 0xFFFFFF00, 0xFF00FF00, 0xFFFF00FF, 0xFFFF8800, 0xFF3366FF, 0xFFFFFFFF
	};

	private final Screen parent;

	public ConfigScreen(Screen parent) {
		super(Component.literal("Seed Ore ESP"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Config c = Config.get();
		int cx = this.width / 2;
		int y = Math.max(8, (this.height - 216) / 2);

		Button title = Button.builder(Component.literal("Seed Ore ESP"), b -> {}).bounds(cx - 150, y, 300, 20).build();
		title.active = false;
		this.addRenderableWidget(title);
		y += 24;

		EditBox seedBox = new EditBox(this.font, cx - 150, y, 300, 20, Component.literal("Seed"));
		seedBox.setMaxLength(64);
		seedBox.setHint(Component.literal("Welt-Seed eingeben (Zahl oder Text)"));
		seedBox.setValue(c.seed == null ? "" : c.seed);
		seedBox.setResponder(s -> {
			c.seed = s.trim();
			Config.touch();
		});
		this.addRenderableWidget(seedBox);
		y += 24;

		Button master = Button.builder(masterLabel(c), b -> {
			c.enabled = !c.enabled;
			b.setMessage(masterLabel(c));
		}).bounds(cx - 150, y, 148, 20).build();
		this.addRenderableWidget(master);
		this.addRenderableWidget(Button.builder(Component.literal("Kalibrieren"),
				b -> OreManager.calibrate(Minecraft.getInstance())).bounds(cx + 2, y, 148, 20).build());
		y += 24;

		this.addRenderableWidget(new RangeSlider(cx - 150, y, 300, 20, c.range));
		y += 24;

		OreType[] types = OreType.values();
		for (int i = 0; i < types.length; i++) {
			OreType t = types[i];
			int x = cx - 150 + (i % 2) * 152;
			int yy = y + (i / 2) * 24;
			Button toggle = Button.builder(typeLabel(c, t), b -> {
				c.typeOn.put(t.name(), !c.typeEnabled(t));
				b.setMessage(typeLabel(c, t));
			}).bounds(x, yy, 104, 20).build();
			Button color = Button.builder(colorLabel(c, t), b -> {
				int cur = c.color(t), next = PALETTE[0];
				for (int k = 0; k < PALETTE.length; k++) {
					if (PALETTE[k] == cur) { next = PALETTE[(k + 1) % PALETTE.length]; break; }
				}
				c.typeColor.put(t.name(), next);
				b.setMessage(colorLabel(c, t));
			}).bounds(x + 106, yy, 42, 20).build();
			this.addRenderableWidget(toggle);
			this.addRenderableWidget(color);
		}
		y += ((types.length + 1) / 2) * 24;

		this.addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> this.onClose())
				.bounds(cx - 150, y, 300, 20).build());
	}

	private static Component masterLabel(Config c) {
		return Component.literal("ESP: " + (c.enabled ? "AN" : "AUS"));
	}

	private static Component typeLabel(Config c, OreType t) {
		return Component.literal(t.label + ": " + (c.typeEnabled(t) ? "AN" : "AUS"));
	}

	private static Component colorLabel(Config c, OreType t) {
		int rgb = c.color(t) & 0xFFFFFF;
		return Component.literal("■■■").withStyle(s -> s.withColor(rgb));
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public void onClose() {
		Config.save();
		this.minecraft.setScreenAndShow(parent);
	}

	/** Sichtweite in Chunks (1-16). */
	private static class RangeSlider extends AbstractSliderButton {
		RangeSlider(int x, int y, int w, int h, int current) {
			super(x, y, w, h, Component.empty(), (Math.max(1, Math.min(16, current)) - 1) / 15.0);
			updateMessage();
		}

		private int get() { return 1 + (int) Math.round(this.value * 15); }

		@Override
		protected void updateMessage() {
			setMessage(Component.literal("Sichtweite: " + get() + " Chunks (" + get() * 16 + " Blöcke)"));
		}

		@Override
		protected void applyValue() {
			Config.get().range = get();
		}
	}
}
