package dev.draxon.module.combat;

import net.minecraft.client.MinecraftClient;

/** Releases forward for Tap Duration after a hit so the next hit is a fresh sprint hit (sprint knockback). */
public class WTap extends TapBase {
    public WTap() { super("W-Tap"); }
    @Override protected void hold(MinecraftClient mc) { mc.options.forwardKey.setPressed(false); }
}
