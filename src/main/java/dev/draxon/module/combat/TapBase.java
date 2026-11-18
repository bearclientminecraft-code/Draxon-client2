package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Sprint-reset tap after a hit (shared by W-Tap and S-Tap).
 * Why it works: the server only gives the extra sprint knockback when a sprint was (re)started; a sprint that is simply held
 * keeps hitting with reduced knockback. Releasing forward for a tick stops the sprint, pressing it again starts a fresh one.
 * Implementation: tick-accurate (input is read once per tick, so durations are whole ticks), triggered by a CONFIRMED entity hit,
 * using the sprint state at the moment of the hit (vanilla clears sprint right after a sprint hit), and a real double tap
 * when sprint is not held by a key (vanilla restarts sprint on a second quick press of forward).
 */
public abstract class TapBase extends Module {
    protected final NumberSetting tap, release;
    protected final DelayManager.Delay delay;
    protected final BooleanSetting smart, onlyAttack, combo, playersOnly, doubleTap;

    private enum Phase { IDLE, WAIT, TAP, LOCKOUT }
    private Phase phase = Phase.IDLE; private long until; private int[] pattern = {1}; private int step, remain;

    protected TapBase(String name, double tapMs, double lockoutMs) {
        super(name, Category.COMBAT, "Sword");
        tap = ms("Tap Duration", tapMs, 50, 500);                 // rounded up to whole ticks (50 ms)
        release = ms("Release Duration", lockoutMs, 0, 1000);     // lockout after a tap before the next one may start
        delay = delay("Delay", 0, 500, "Random Delay", 5, 100);
        smart = bool("Smart Timing", true); onlyAttack = bool("Only While Attacking", true); combo = bool("Combo Detection", false);
        playersOnly = bool("Players Only", false); doubleTap = bool("Double Tap Fallback", true);
    }

    /** First phase of the tap (W-Tap: release forward; S-Tap: release forward and press back). */
    protected abstract void hold(MinecraftClient mc);
    protected void freeKeys(MinecraftClient mc) { Keys.restoreMovement(mc); }

    @Override protected void onDisable() { abort(MinecraftClient.getInstance()); }
    @Override public void reset() { phase = Phase.IDLE; CombatState.releaseMove(this); }
    @Override public void validate() { reset(); }
    private void abort(MinecraftClient mc) { if (phase == Phase.TAP && mc != null && mc.player != null) freeKeys(mc); reset(); }

    private boolean smartOk(MinecraftClient mc, ClientPlayerEntity p) { return Keys.physical(mc, mc.options.forwardKey) && p.hurtTime == 0; }

    private boolean trigger(MinecraftClient mc, ClientPlayerEntity p) {
        if (onlyAttack.get()) {
            if (!CombatState.hitThisTick() || !CombatState.sprintAtHit()) return false;
            if (playersOnly.get() && !(CombatState.hitTarget() instanceof PlayerEntity)) return false;
        } else {
            LivingEntity t = CombatState.activeTarget();
            if (!p.isSprinting() || t == null || Geo.eyeDistance(p, t) > 3.5 || !CombatState.attackReady(p, 0.8f)) return false;
            if (playersOnly.get() && !(t instanceof PlayerEntity)) return false;
        }
        if (combo.get() && CombatState.combo < 2) return false;
        return !smart.get() || smartOk(mc, p);
    }

    private void apply(MinecraftClient mc) {
        if (step == 0) hold(mc);
        else if (step == 2) mc.options.forwardKey.setPressed(false); // second tap of the double-tap fallback
        else freeKeys(mc);
    }
    private void beginTap(MinecraftClient mc) {
        int ticks = Math.max(1, (int) Math.ceil(tap.get() / 50.0));
        boolean sprintHeld = mc.options.sprintKey.isPressed() || Keys.physical(mc, mc.options.sprintKey);
        pattern = doubleTap.get() && !sprintHeld ? new int[]{ticks, 1, 1} : new int[]{ticks};
        int total = 0; for (int t : pattern) total += t;
        if (!CombatState.acquireMove(this, total * 50L + 200)) { phase = Phase.IDLE; return; }
        step = 0; remain = pattern[0]; phase = Phase.TAP;
        apply(mc); remain--; // the override is active for the input read of THIS tick
    }

    @Override public void onPreTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) { abort(mc); return; }
        long now = System.currentTimeMillis();
        switch (phase) {
            case IDLE -> {
                if (!trigger(mc, p)) return;
                long d = delay.roll();
                if (d <= 0) beginTap(mc); else { until = now + d; phase = Phase.WAIT; }
            }
            case WAIT -> {
                if (smart.get() && !smartOk(mc, p)) phase = Phase.IDLE;
                else if (now >= until) beginTap(mc);
            }
            case TAP -> {
                if (remain > 0) { apply(mc); remain--; return; }
                step++;
                if (step >= pattern.length) { freeKeys(mc); CombatState.releaseMove(this); phase = Phase.LOCKOUT; until = now + release.get().longValue(); return; }
                remain = pattern[step]; apply(mc); remain--;
            }
            case LOCKOUT -> { if (now >= until) phase = Phase.IDLE; }
        }
    }
}
