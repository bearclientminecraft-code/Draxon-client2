package dev.draxon.combat;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/**
 * One rotation goal at a time (highest priority wins). Modules refresh the goal every tick; the view is then moved
 * EVERY FRAME toward it, so the camera glides at the monitor's refresh rate instead of stepping at 20 Hz.
 * The goal angles are re-evaluated per frame from interpolated positions (see Aim), so a moving target is tracked smoothly.
 */
public final class RotationManager {
    /** Computes the wanted yaw/pitch for the current frame; return null when the goal is lost. */
    public interface Aim { float[] angles(MinecraftClient mc, ClientPlayerEntity p, float partialTick); }

    private static Aim aim; private static int prio, ttl;
    private static float hSpeed, vSpeed, smooth, strength = 1f, yield;
    private static float lastYaw = Float.NaN, lastPitch;
    private RotationManager() {}

    public static void init() { HudRenderCallback.EVENT.register((ctx, counter) -> frame(MinecraftClient.getInstance(), counter)); }

    /**
     * @param smoothing 0..0.95 per-tick easing (higher = slower); speeds are max degrees per tick.
     * @param strength  0..1 how much of the computed correction is applied (1 = full pull, 0.3 = gentle nudge).
     * @param mouseYield 0..1 how much the pull is reduced while the player is turning the camera himself (0 = never yield).
     * Call every tick to keep the goal alive.
     */
    public static void request(int priority, Aim a, float hSpeedDeg, float vSpeedDeg, float smoothing, float strength, float mouseYield) {
        if (ttl > 0 && priority < prio) return;
        aim = a; prio = priority; hSpeed = hSpeedDeg; vSpeed = vSpeedDeg; smooth = smoothing;
        RotationManager.strength = MathHelper.clamp(strength, 0f, 1f); RotationManager.yield = MathHelper.clamp(mouseYield, 0f, 1f); ttl = 2;
    }
    public static boolean pending() { return ttl > 0; }
    public static void clear() { aim = null; ttl = 0; lastYaw = Float.NaN; }
    /** Called once per tick after all modules ran. */
    public static void endTick() { if (ttl > 0 && --ttl == 0) { aim = null; lastYaw = Float.NaN; } }

    private static void frame(MinecraftClient mc, RenderTickCounter counter) {
        ClientPlayerEntity p = mc.player;
        if (ttl <= 0 || aim == null || p == null || mc.currentScreen != null) return;
        float dt = MathHelper.clamp(counter.getDynamicDeltaTicks(), 0f, 3f); // ticks elapsed since the previous frame
        if (dt <= 0f) return;
        float[] a = aim.angles(mc, p, counter.getTickProgress(false));
        if (a == null) return;
        float dy = MathHelper.wrapDegrees(a[0] - p.getYaw()), dp = a[1] - p.getPitch();
        float alpha = 1f - (float) Math.pow(MathHelper.clamp(smooth, 0f, 0.95f), dt); // frame-rate independent easing
        // Mouse priority: the faster the player turns the camera himself (deg/tick, measured since our last frame), the weaker the pull.
        float user = Float.isNaN(lastYaw) ? 0f : (float) Math.hypot(MathHelper.wrapDegrees(p.getYaw() - lastYaw), p.getPitch() - lastPitch) / dt;
        float pull = strength * (1f - yield * MathHelper.clamp(user / 8f, 0f, 1f));
        float sy = MathHelper.clamp(dy * alpha * pull, -hSpeed * dt, hSpeed * dt), sp = MathHelper.clamp(dp * alpha * pull, -vSpeed * dt, vSpeed * dt);
        if (Math.abs(sy) >= 0.005f || Math.abs(sp) >= 0.005f)
            p.changeLookDirection(sy / 0.15, sp / 0.15); // same path as the mouse: keeps previous-angle interpolation consistent
        lastYaw = p.getYaw(); lastPitch = p.getPitch();
    }
}
