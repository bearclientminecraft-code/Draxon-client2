package dev.draxon.module.combat;

import dev.draxon.combat.CombatState;
import dev.draxon.combat.DelayManager;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/**
 * While the use key is held with an end crystal in hand, attacks an end crystal under the crosshair
 * (the one just placed). Interval = Hit Delay +- Random Delay, clamped to 1000/MaxCPS .. 1000/MinCPS.
 * Crystal Slot 0 = any hotbar slot holding a crystal; 1-9 = only while that slot is selected.
 */
public class AutoHitCrystal extends Module {
    private final NumberSetting crystalSlot = add(new NumberSetting("Crystal Slot", 0, 0, 9, 1));
    private final NumberSetting minCps = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
    private final NumberSetting maxCps = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
    private final DelayManager.Delay delay = delay("Hit Delay", 50, 500, "Random Delay", 5, 100);
    private final DelayManager.Timer timer = new DelayManager.Timer();

    public AutoHitCrystal() { super("AutoHitCrystal", Category.COMBAT, "Crystal"); }
    @Override protected void onDisable() { timer.reset(); }
    @Override public void reset() { timer.reset(); }
    @Override public void validate() { if (minCps.get() > maxCps.get()) { double a = minCps.get(); minCps.set(maxCps.get()); maxCps.set(a); } }

    private long interval() {
        double lo = 1000.0 / Math.max(minCps.get(), maxCps.get()), hi = 1000.0 / Math.min(minCps.get(), maxCps.get());
        return (long) Math.max(lo, Math.min(hi, delay.roll()));
    }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (!mc.options.useKey.isPressed() || !p.getMainHandStack().isOf(Items.END_CRYSTAL)) return;
        if (crystalSlot.get() > 0 && p.getInventory().getSelectedSlot() != crystalSlot.get().intValue() - 1) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult h) || !(h.getEntity() instanceof EndCrystalEntity c) || !c.isAlive()) return;
        if (!timer.passed() || !CombatState.claimAttack()) return;
        mc.interactionManager.attackEntity(p, c);
        p.swingHand(Hand.MAIN_HAND);
        timer.start(interval());
    }
}
