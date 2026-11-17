package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;

/** Holds the sprint key while conditions allow. Yields to W-Tap / S-Tap / Sprint Reset via the movement lock. */
public class AutoSprint extends Module {
    private final BooleanSetting always = bool("Always Sprint", true), swordOnly = bool("Sword Only", false),
            pauseEat = bool("Pause While Eating", true), pauseBlock = bool("Pause While Blocking", true),
            pauseInv = bool("Pause While Inventory Open", true), integration = bool("Sprint Reset Integration", true);

    public AutoSprint() { super("AutoSprint", Category.COMBAT, "Sword"); }
    @Override protected void onDisable() { MinecraftClient mc = MinecraftClient.getInstance(); if (mc.player != null) Keys.restore(mc, mc.options.sprintKey); }

    @Override public void onPreTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        boolean pause = !always.get()
                || (swordOnly.get() && !CombatState.sword(p))
                || (pauseEat.get() && p.isUsingItem() && !p.isBlocking())
                || (pauseBlock.get() && p.isBlocking())
                || (pauseInv.get() && mc.currentScreen instanceof HandledScreen<?>)
                || (mc.currentScreen != null && !(mc.currentScreen instanceof HandledScreen<?>))
                || (integration.get() && !CombatState.moveFree(this));
        if (pause) { Keys.restore(mc, mc.options.sprintKey); return; }
        mc.options.sprintKey.setPressed(true);
    }
}
