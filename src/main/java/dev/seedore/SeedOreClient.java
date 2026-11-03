package dev.seedore;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SeedOreClient implements ClientModInitializer {
	public static KeyMapping openGui;
	public static KeyMapping toggle;

	@Override
	public void onInitializeClient() {
		Config.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("seedoresp", "main"));
		openGui = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.seedoresp.gui", InputConstants.KEY_O, category));
		toggle = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.seedoresp.toggle", InputConstants.KEY_P, category));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openGui.consumeClick()) {
				if (mc.gui.screen() == null) mc.setScreenAndShow(new ConfigScreen(null));
			}
			while (toggle.consumeClick()) {
				Config c = Config.get();
				c.enabled = !c.enabled;
				Config.save();
				if (mc.player != null) mc.player.displayClientMessage(Component.literal("Seed Ore ESP: " + (c.enabled ? "AN" : "AUS")), true);
				if (c.enabled && c.seedValue() == null) {
					OreManager.chat(mc, "Noch kein Seed gesetzt - Menü mit der Öffnen-Taste (Standard: O).");
				}
			}
			OreManager.tick(mc);
		});

		// Gizmos werden pro Frame neu eingereicht; "alwaysOnTop" = durch Wände sichtbar.
		LevelRenderEvents.BEFORE_GIZMOS.register(ctx -> OreManager.render(Minecraft.getInstance()));
	}
}
