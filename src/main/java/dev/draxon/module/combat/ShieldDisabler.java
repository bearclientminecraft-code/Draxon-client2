package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Switches to an axe while the target under the crosshair is blocking with a shield and returns to the sword afterwards.
 * It only switches the hotbar slot; the hit itself comes from TriggerBot / AutoClicker / the player (CombatState.weaponOk allows the axe).
 */
public class ShieldDisabler extends Module {
    private final BooleanSetting shieldDetect = bool("Shield Detection", true), axeDetect = bool("Axe Detection", true),
            weaponSwitch = bool("Weapon Switch", true), returnSword = bool("Return To Sword", true),
            validation = bool("Target Validation", true), rangeCheck = bool("Range Check", true);
    private final NumberSetting axeSlot = add(new NumberSetting("Axe Slot", 2, 1, 9, 1));
    private final NumberSetting range = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final dev.draxon.combat.DelayManager.Delay switchDelay = delay("Switch Delay", 50, 500, "Switch Random Delay", 5, 100);
    private final dev.draxon.combat.DelayManager.Delay returnDelay = delay("Return Delay", 100, 1000, "Return Random Delay", 5, 100);

    private int previousSlot = -1; private long switchDue, returnDue;

    public ShieldDisabler() { super("ShieldDisabler", Category.COMBAT, "Sword"); axeSlot.visibleIf(() -> !axeDetect.get()); }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { previousSlot = -1; switchDue = returnDue = 0; }

    private static boolean shielded(LivingEntity t, boolean needBlocking) {
        return needBlocking ? t.isBlocking() : t.getMainHandStack().isOf(Items.SHIELD) || t.getOffHandStack().isOf(Items.SHIELD);
    }
    private int axe(ClientPlayerEntity p) {
        if (!axeDetect.get()) { int s = axeSlot.get().intValue() - 1; return p.getInventory().getStack(s).isIn(ItemTags.AXES) ? s : -1; }
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isIn(ItemTags.AXES)) return i;
        return -1;
    }
    private int sword(ClientPlayerEntity p) {
        if (previousSlot >= 0 && p.getInventory().getStack(previousSlot).isIn(ItemTags.SWORDS)) return previousSlot;
        for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).isIn(ItemTags.SWORDS)) return i;
        return -1;
    }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null) return;
        LivingEntity t = mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity le ? le : null;
        boolean tOk = t != null && TargetManager.valid(p, t, validation.get(), false) && (!rangeCheck.get() || Geo.eyeDistance(p, t) <= range.get());
        boolean blocking = tOk && shielded(t, shieldDetect.get());
        long now = System.currentTimeMillis();

        if (previousSlot >= 0) { // currently on the axe
            if (!returnSword.get()) { previousSlot = -1; return; }
            if (CombatState.attackedThisTick() || !blocking) { if (returnDue == 0) returnDue = now + returnDelay.roll(); }
            else returnDue = 0;
            if (returnDue != 0 && now >= returnDue) {
                int s = sword(p); if (s >= 0) p.getInventory().setSelectedSlot(s);
                previousSlot = -1; returnDue = 0;
            }
            return;
        }
        if (!weaponSwitch.get() || !blocking || !CombatState.sword(p)) { switchDue = 0; return; }
        if (switchDue == 0) switchDue = now + switchDelay.roll();
        if (now < switchDue) return;
        int a = axe(p);
        if (a >= 0) { previousSlot = p.getInventory().getSelectedSlot(); p.getInventory().setSelectedSlot(a); }
        switchDue = 0;
    }
}
