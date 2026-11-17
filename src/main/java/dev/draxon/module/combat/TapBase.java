package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

/**
 * Shared state machine of W-Tap and S-Tap: trigger -> wait (Delay +- Random) -> hold the override for Tap Duration ->
 * lockout for Release Duration. Key overrides happen in onPreTick, right before the game reads movement input.
 */
public abstract class TapBase extends Module {
    protected final NumberSetting tap = ms("Tap Duration", 100, 20, 500), release = ms("Release Duration", 150, 0, 1000);
    protected final DelayManager.Delay delay = delay("Delay", 0, 500, "Random Delay", 5, 100);
    protected final BooleanSetting smart = bool("Smart Timing", true), onlyAttack = bool("Only While Attacking", true), combo = bool("Combo Detection", false);

    private enum Phase { IDLE, WAIT, TAP, LOCKOUT }
    private Phase phase = Phase.IDLE; private long until;

    protected TapBase(String name) { super(name, Category.COMBAT, "Sword"); }
    /** Apply the key override for this tick. */
    protected abstract void hold(MinecraftClient mc);
    /** Give the keys back to the player. */
    protected void freeKeys(MinecraftClient mc) { Keys.restoreMovement(mc); }

    @Override protected void onDisable() { abort(MinecraftClient.getInstance()); }
    @Override public void reset() { phase = Phase.IDLE; CombatState.releaseMove(this); }
    private void abort(MinecraftClient mc) {
        if (phase == Phase.TAP && mc != null && mc.player != null) freeKeys(mc);
        reset();
    }
    @Override public void validate() { reset(); }

    private boolean smartOk(MinecraftClient mc, ClientPlayerEntity p) {
        return p.isSprinting() && Keys.physical(mc, mc.options.forwardKey) && p.hurtTime == 0;
    }
    private boolean trigger(MinecraftClient mc, ClientPlayerEntity p) {
        if (!p.isSprinting()) return false;
        if (combo.get() && CombatState.combo < 2) return false;
        if (smart.get() && !smartOk(mc, p)) return false;
        if (onlyAttack.get()) return CombatState.attackedThisTick();
        LivingEntity t = CombatState.activeTarget();
        return t != null && Geo.eyeDistance(p, t) <= 3.5 && CombatState.attackReady(p, 0.8f);
    }

    @Override public void onPreTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) { abort(mc); return; }
        long now = System.currentTimeMillis();
        switch (phase) {
            case IDLE -> { if (trigger(mc, p)) { until = now + delay.roll(); phase = Phase.WAIT; } }
            case WAIT -> {
                if (smart.get() && !smartOk(mc, p)) { phase = Phase.IDLE; }
                else if (now >= until) {
                    if (CombatState.acquireMove(this, (long) tap.get().doubleValue() + 100)) { until = now + tap.get().longValue(); phase = Phase.TAP; hold(mc); }
                    else phase = Phase.IDLE;
                }
            }
            case TAP -> {
                if (now >= until) { freeKeys(mc); CombatState.releaseMove(this); until = now + release.get().longValue(); phase = Phase.LOCKOUT; }
                else hold(mc);
            }
            case LOCKOUT -> { if (now >= until) phase = Phase.IDLE; }
        }
    }
}
