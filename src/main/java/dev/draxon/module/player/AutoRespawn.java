package dev.draxon.module.player;

import dev.draxon.combat.DelayManager;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;

/** Presses Respawn on the death screen after Respawn Delay +- Random Delay. Hardcore worlds are skipped (no respawn there). */
public class AutoRespawn extends Module {
    private final DelayManager.Delay delay = delay("Respawn Delay", 300, 3000, "Random Delay", 50, 1000);
    private final BooleanSetting skipHardcore = bool("Skip Hardcore", true);
    private long due; private boolean done;

    public AutoRespawn() { super("AutoRespawn", Category.PLAYER); }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { due = 0; done = false; }

    @Override public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || !(mc.currentScreen instanceof DeathScreen) || !mc.player.isDead()) { reset(); return; }
        if (done || (skipHardcore.get() && mc.world.getLevelProperties().isHardcore())) return;
        long now = System.currentTimeMillis();
        if (due == 0) due = now + delay.roll();
        if (now >= due) { mc.player.requestRespawn(); done = true; }
    }
}
