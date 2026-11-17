package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

/**
 * Attack gate used by TriggerBot and AutoClicker (via CombatState.gate). An attack is allowed only while the timing
 * condition has held for a rolled Delay +- Random Delay.
 */
public class HitSelect extends Module {
    private final ModeSetting timing = mode("Timing", "Both", "Target Vulnerable", "Full Cooldown", "Both");
    private final DelayManager.Delay delay = delay("Delay", 0, 300, "Random Delay", 5, 100);
    private final BooleanSetting targetCheck = bool("Target Check", true), weaponCheck = bool("Weapon Check", true), cooldownCheck = bool("Cooldown Check", true);

    private long readySince, wait;
    public HitSelect() { super("HitSelect", Category.COMBAT, "Sword"); }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { readySince = 0; }
    @Override public void onTick(MinecraftClient mc) { if (CombatState.attackedThisTick()) readySince = 0; }

    public boolean allow(MinecraftClient mc, LivingEntity t) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return false;
        if (targetCheck.get() && !TargetManager.valid(p, t, true, false)) return false;
        if (weaponCheck.get() && !CombatState.weaponOk(p)) return false;
        if (cooldownCheck.get() && !CombatState.attackReady(p, 0.95f)) { readySince = 0; return false; }
        boolean vulnerable = t.timeUntilRegen <= 10, full = CombatState.attackReady(p, 1.0f);
        boolean ok = switch (timing.get()) { case "Target Vulnerable" -> vulnerable; case "Full Cooldown" -> full; default -> vulnerable && full; };
        if (!ok) { readySince = 0; return false; }
        long now = System.currentTimeMillis();
        if (readySince == 0) { readySince = now; wait = delay.roll(); }
        return now - readySince >= wait;
    }
}
