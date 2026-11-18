package dev.draxon.module.combat;

import net.minecraft.client.MinecraftClient;

/** Releases forward and presses back for Tap Duration after a hit to open distance and reset sprint. */
public class STap extends TapBase {
    public STap() { super("S-Tap", 100, 250); }
    @Override protected void hold(MinecraftClient mc) { mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(true); }
}
