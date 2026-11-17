package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.core.DraxonClient;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.ModeSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Queues vanilla clicks (so block breaking, misses and cooldown behave like real clicks).
 * Interval = Click Delay +- Random Delay, clamped to 1000/MaxCPS .. 1000/MinCPS.
 */
public class AutoClicker extends Module {
    private final NumberSetting minCps = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
    private final DelayManager.Delay delay = delay("Click Delay", 80, 500, "Random Delay", 5, 100);
    private final BooleanSetting left = bool("Left Click", true), right = bool("Right Click", false),
            weaponCheck = bool("Weapon Check", true), swordOnly = bool("Only While Holding Sword", false);
    private final ModeSetting holdMode = mode("Click Hold Mode", "Hold", "Hold", "Always");

    private final DelayManager.Timer leftTimer = new DelayManager.Timer(), rightTimer = new DelayManager.Timer();

    public AutoClicker() { super("AutoClicker", Category.COMBAT, "Sword"); }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { leftTimer.reset(); rightTimer.reset(); }
    @Override public void validate() { if (minCps.get() > maxCps.get()) { double a = minCps.get(); minCps.set(maxCps.get()); maxCps.set(a); } }

    private long interval() {
        double lo = 1000.0 / Math.max(minCps.get(), maxCps.get()), hi = 1000.0 / Math.min(minCps.get(), maxCps.get());
        return (long) Math.max(lo, Math.min(hi, delay.roll()));
    }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null) return;
        if (weaponCheck.get() && !CombatState.weaponOk(p)) return;
        if (swordOnly.get() && !CombatState.sword(p)) return;
        boolean always = holdMode.get().equals("Always");
        if (left.get() && (always || mc.options.attackKey.isPressed()) && !p.isUsingItem() && leftTimer.passed()) {
            boolean entity = mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity;
            TriggerBot tb = DraxonClient.modules.get(TriggerBot.class);
            if (entity && tb != null && tb.isEnabled()) { /* TriggerBot owns entity attacks */ }
            else if (!entity || (CombatState.gate(mc, (LivingEntity) ((EntityHitResult) mc.crosshairTarget).getEntity()) && CombatState.claimAttack())) {
                Keys.click(mc.options.attackKey); leftTimer.start(interval());
            }
        }
        if (right.get() && (always || mc.options.useKey.isPressed()) && rightTimer.passed()) {
            Keys.click(mc.options.useKey); rightTimer.start(interval());
        }
    }
}
