package dev.draxon.module.movement;

import dev.draxon.combat.CombatState;
import dev.draxon.combat.Keys;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/** Holds the sprint key so you sprint as soon as you move forward. Vanilla rules (hunger, blindness, ...) still apply. */
public class Sprint extends Module {
    private final BooleanSetting onlyForward = bool("Only While Moving Forward", true), pauseEat = bool("Pause While Eating", true),
            pauseBlock = bool("Pause While Blocking", true), pauseScreens = bool("Pause In Screens", true),
            combatLock = bool("Respect Combat Resets", true);

    public Sprint() { super("Sprint", Category.MOVEMENT); }
    @Override protected void onDisable() { MinecraftClient mc = MinecraftClient.getInstance(); if (mc.player != null) Keys.restore(mc, mc.options.sprintKey); }

    @Override public void onPreTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        boolean pause = (onlyForward.get() && !Keys.physical(mc, mc.options.forwardKey))
                || (pauseEat.get() && p.isUsingItem() && !p.isBlocking())
                || (pauseBlock.get() && p.isBlocking())
                || (pauseScreens.get() && mc.currentScreen != null)
                || (combatLock.get() && !CombatState.moveFree(this)); // W-Tap / S-Tap / Sprint Reset own the keys
        if (pause) { Keys.restore(mc, mc.options.sprintKey); return; }
        mc.options.sprintKey.setPressed(true);
    }
}
