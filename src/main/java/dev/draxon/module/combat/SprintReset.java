package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

/**
 * Stops and restarts sprinting around a hit so the next hit carries sprint knockback.
 * Key mode: sprint key released for one tick. Packet mode: STOP_SPRINTING + START_SPRINTING (the same packets vanilla sends).
 */
public class SprintReset extends Module {
    private final ModeSetting resetMode = mode("Reset Mode", "Key", "Key", "Packet");
    private final ModeSetting timing = mode("Reset Timing", "On Attack", "On Attack", "On Hit Received");
    private final DelayManager.Delay delay = delay("Reset Delay", 0, 500, "Random Delay", 5, 100);
    private final BooleanSetting smart = bool("Smart Reset", true), onlyAttack = bool("Only While Attacking", true),
            swordCheck = bool("Sword Check", true), moveCheck = bool("Movement Check", true);

    private boolean pending, restore; private long due;
    public SprintReset() { super("SprintReset", Category.COMBAT, "Sword"); }
    @Override protected void onDisable() { reset(); Keys.restore(MinecraftClient.getInstance(), MinecraftClient.getInstance().options.sprintKey); }
    @Override public void reset() { pending = restore = false; CombatState.releaseMove(this); }

    @Override public void onPreTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) { reset(); return; }
        if (restore) { Keys.restore(mc, mc.options.sprintKey); CombatState.releaseMove(this); restore = false; return; }
        long now = System.currentTimeMillis();
        if (!pending) {
            boolean onAttack = timing.get().equals("On Attack");
            boolean ev = onAttack ? CombatState.hitThisTick() : CombatState.hurtThisTick();
            // vanilla clears sprint right after a sprint hit, so the state at the moment of the hit is what counts
            if (!ev || !(onAttack ? CombatState.sprintAtHit() : p.isSprinting())) return;
            if (swordCheck.get() && !CombatState.sword(p)) return;
            if (moveCheck.get() && !Keys.physical(mc, mc.options.forwardKey)) return;
            if (onlyAttack.get() && !CombatState.attackedRecently(1500)) return;
            if (smart.get()) {
                LivingEntity t = CombatState.activeTarget();
                if (t == null || Geo.eyeDistance(p, t) > 4.5 || !CombatState.moveFree(this)) return;
            }
            pending = true; due = now + delay.roll(); return;
        }
        if (now < due) return;
        pending = false;
        if (!CombatState.acquireMove(this, 200)) return;
        if (resetMode.get().equals("Packet") && p.networkHandler != null) {
            p.networkHandler.sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            p.networkHandler.sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.START_SPRINTING));
            CombatState.releaseMove(this);
        } else {
            p.setSprinting(false); mc.options.sprintKey.setPressed(false); restore = true; // keys come back next tick
        }
    }
    @Override public void validate() { reset(); }
}
