package dev.draxon.combat;

import dev.draxon.core.DraxonClient;
import dev.draxon.module.combat.ShieldDisabler;
import dev.draxon.module.combat.HitSelect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Shared combat state, updated once per tick (START_CLIENT_TICK) before any module runs.
 * Attacks are detected from the attack-cooldown reset, so manual and module attacks are both seen.
 */
public final class CombatState {
    private static long tick = 0, attackTick = -1, hurtTick = -1, claimTick = -1, lastAttackMs, lastTargetMs;
    private static float lastCooldown = 1f; private static int lastHurt, lastSlot = -1;
    private static LivingEntity target; public static int combo;
    private static Object moveOwner; private static long moveUntil;
    private CombatState() {}

    public static void update(MinecraftClient mc) {
        tick++;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) { reset(); return; }
        int slot = p.getInventory().getSelectedSlot();
        float cd = p.getAttackCooldownProgress(0f);
        if (slot == lastSlot && cd < lastCooldown - 0.2f) { // slot change also resets the cooldown: ignored
            attackTick = tick; lastAttackMs = System.currentTimeMillis();
            if (mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity le) { target = le; lastTargetMs = lastAttackMs; combo++; }
        }
        lastSlot = slot; lastCooldown = cd;
        if (p.hurtTime > lastHurt) { hurtTick = tick; combo = 0; }
        lastHurt = p.hurtTime;
        if (target != null && !target.isAlive()) { target = null; combo = 0; }
    }
    public static void reset() { target = null; combo = 0; attackTick = hurtTick = claimTick = -1; lastCooldown = 1f; lastHurt = 0; lastSlot = -1; moveOwner = null; }

    public static boolean attackedThisTick() { return attackTick == tick; }
    public static boolean hurtThisTick() { return hurtTick == tick; }
    /** True if the player attacked within the last {@code ms}. */
    public static boolean attackedRecently(long ms) { return System.currentTimeMillis() - lastAttackMs < ms; }
    public static LivingEntity activeTarget() {
        return target != null && target.isAlive() && System.currentTimeMillis() - lastTargetMs < 3000 ? target : null;
    }
    public static void setTarget(LivingEntity e) { target = e; lastTargetMs = System.currentTimeMillis(); }

    /** Only one module may start an attack per tick. */
    public static boolean claimAttack() { if (claimTick == tick) return false; claimTick = tick; return true; }

    /** Movement/sprint lock so W-Tap, S-Tap, Sprint Reset, Target Strafe and Auto Sprint never fight over the keys. */
    public static boolean acquireMove(Object owner, long ms) {
        if (moveOwner != null && moveOwner != owner && System.currentTimeMillis() < moveUntil) return false;
        moveOwner = owner; moveUntil = System.currentTimeMillis() + ms; return true;
    }
    public static void releaseMove(Object owner) { if (moveOwner == owner) moveOwner = null; }
    public static boolean moveFree(Object asker) { return moveOwner == null || moveOwner == asker || System.currentTimeMillis() >= moveUntil; }

    public static boolean sword(ClientPlayerEntity p) { return p.getMainHandStack().isIn(ItemTags.SWORDS); }
    /** Sword, or an axe while Shield Disabler has switched to it. */
    public static boolean weaponOk(ClientPlayerEntity p) {
        if (sword(p)) return true;
        ShieldDisabler sd = DraxonClient.modules == null ? null : DraxonClient.modules.get(ShieldDisabler.class);
        return sd != null && sd.isEnabled() && p.getMainHandStack().isIn(ItemTags.AXES);
    }
    public static boolean attackReady(ClientPlayerEntity p, float min) { return p.getAttackCooldownProgress(0.5f) >= min; }
    /** Hit Select gate (always true when Hit Select is off). */
    public static boolean gate(MinecraftClient mc, LivingEntity t) {
        HitSelect hs = DraxonClient.modules == null ? null : DraxonClient.modules.get(HitSelect.class);
        return hs == null || !hs.isEnabled() || hs.allow(mc, t);
    }
}
