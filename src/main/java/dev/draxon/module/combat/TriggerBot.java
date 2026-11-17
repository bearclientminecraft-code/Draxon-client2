package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Random;

/** Attacks the valid target under the crosshair (or the best valid target in FOV when Visibility Check is off). */
public class TriggerBot extends Module {
    private final NumberSetting hitChance = pct("Hit Chance", 100, 1, 100);
    private final DelayManager.Delay delay = delay("Delay", 100, 500, "Random Delay", 5, 100);
    private final NumberSetting range = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final NumberSetting fov = add(new NumberSetting("FOV", 90, 1, 360, 1).unit("\u00B0"));
    private final BooleanSetting weaponCheck = bool("Weapon Check", true), visibility = bool("Visibility Check", true),
            cooldown = bool("Cooldown Check", true), critsOnly = bool("Criticals Only", false),
            holdClick = bool("Only While Holding Click", false), validation = bool("Target Validation", true),
            ignoreTeam = bool("Ignore Teammates", true);

    private final Random rng = new Random();
    private final TargetManager.Selector selector = new TargetManager.Selector();
    private long nextAt; private LivingEntity last; private boolean clickWasDown;

    public TriggerBot() { super("TriggerBot", Category.COMBAT, "Sword"); }
    @Override protected void onEnable() { reset(); }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { nextAt = 0; last = null; clickWasDown = false; selector.clear(); }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        boolean down = mc.options.attackKey.isPressed(), just = down && !clickWasDown;
        clickWasDown = down; // rising edge belongs to the vanilla attack of that tick
        if (weaponCheck.get() && !CombatState.weaponOk(p)) return;
        if (holdClick.get() && (!down || just)) return;
        if (p.isUsingItem()) return;

        LivingEntity t = acquire(mc, p);
        if (t == null) { last = null; return; }
        long now = System.currentTimeMillis();
        if (t != last) { last = t; nextAt = Math.max(nextAt, now + delay.roll() / 2); } // target switch: half a delay first
        if (now < nextAt) return;
        if (cooldown.get() && !CombatState.attackReady(p, 0.95f)) return;
        if (critsOnly.get() && !canCrit(p)) return;
        if (!CombatState.gate(mc, t)) return;

        nextAt = now + delay.roll(); // delay is spent even if the Hit Chance roll fails
        if (rng.nextInt(100) >= hitChance.get().intValue()) return;
        if (!CombatState.claimAttack()) return;
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        CombatState.setTarget(t);
    }

    private TargetManager.Req req() {
        return new TargetManager.Req(range.get(), fov.get(), visibility.get(), validation.get(), ignoreTeam.get(), TargetManager.Priority.DISTANCE, 0.5);
    }
    private LivingEntity acquire(MinecraftClient mc, ClientPlayerEntity p) {
        TargetManager.Req r = req();
        if (visibility.get()) { // crosshairTarget already accounts for blocks on the line of sight
            return mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity le && TargetManager.accepts(mc, p, le, r) ? le : null;
        }
        return selector.select(mc, p, r, 0);
    }
    /** Vanilla critical conditions (falling, not climbing/swimming/riding/blind). */
    private static boolean canCrit(ClientPlayerEntity p) {
        return p.fallDistance > 0.0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasVehicle() && !p.hasStatusEffect(StatusEffects.BLINDNESS);
    }
    @Override public void validate() { selector.clear(); }
}
