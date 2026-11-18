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
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Random;

/**
 * Breaks shields: when the target under the crosshair blocks with a shield the module switches to an axe, hits (Auto Hit),
 * then returns to the sword. Each hit attempt has a Miss Chance to be skipped. An axe hit disables the shield even at low
 * attack cooldown, so Auto Hit does not wait for the cooldown unless Cooldown Check is on.
 */
public class ShieldDisabler extends Module {
    private final BooleanSetting shieldDetect = bool("Shield Detection", true), axeDetect = bool("Axe Detection", true),
            weaponSwitch = bool("Weapon Switch", true), returnSword = bool("Return To Sword", true),
            validation = bool("Target Validation", true), rangeCheck = bool("Range Check", true);
    private final NumberSetting axeSlot = add(new NumberSetting("Axe Slot", 2, 1, 9, 1));
    private final NumberSetting range = add(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));
    private final DelayManager.Delay switchDelay = delay("Switch Delay", 50, 500, "Switch Random Delay", 5, 100);
    private final DelayManager.Delay returnDelay = delay("Return Delay", 100, 1000, "Return Random Delay", 5, 100);
    private final BooleanSetting autoHit = bool("Auto Hit", true), cooldownCheck = bool("Cooldown Check", false);
    private final DelayManager.Delay hitDelay = delay("Hit Delay", 60, 500, "Hit Random Delay", 5, 100);
    private final NumberSetting missChance = pct("Miss Chance", 10, 0, 100); // chance that a hit attempt is skipped

    private final Random rng = new Random();
    private int previousSlot = -1; private long switchDue, returnDue, hitDue;

    public ShieldDisabler() {
        super("ShieldDisabler", Category.COMBAT, "Sword");
        axeSlot.visibleIf(() -> !axeDetect.get());
        hitDelay.base.visibleIf(autoHit::get); hitDelay.random.visibleIf(autoHit::get); missChance.visibleIf(autoHit::get); cooldownCheck.visibleIf(autoHit::get);
    }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { previousSlot = -1; switchDue = returnDue = hitDue = 0; }

    /** True while the module has put an axe in the hand (CombatState.weaponOk then accepts the axe). */
    public boolean isSwitched() { return previousSlot >= 0; }

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
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        LivingEntity t = mc.crosshairTarget instanceof EntityHitResult h && h.getEntity() instanceof LivingEntity le ? le : null;
        boolean tOk = t != null && TargetManager.valid(p, t, validation.get(), false) && (!rangeCheck.get() || Geo.eyeDistance(p, t) <= range.get());
        boolean blocking = tOk && shielded(t, shieldDetect.get());
        long now = System.currentTimeMillis();
        boolean axeInHand = p.getMainHandStack().isIn(ItemTags.AXES);

        // Auto Hit: the shield is up and an axe is in the hand (switched by us, or by the player)
        if (autoHit.get() && blocking && axeInHand && !p.isUsingItem()) {
            if (hitDue == 0) hitDue = now + hitDelay.roll();
            if (now >= hitDue) {
                hitDue = now + hitDelay.roll();                        // next attempt, whether this one happens or not
                boolean skip = rng.nextInt(100) < missChance.get().intValue();
                boolean ready = !cooldownCheck.get() || CombatState.attackReady(p, 0.9f);
                if (!skip && ready && CombatState.claimAttack()) { mc.interactionManager.attackEntity(p, t); p.swingHand(Hand.MAIN_HAND); CombatState.setTarget(t); }
            }
        } else hitDue = 0;

        if (previousSlot >= 0) { // currently on the axe: go back once the hit happened or the shield is gone
            if (!returnSword.get()) { previousSlot = -1; return; }
            if (CombatState.attackedThisTick() || !blocking) { if (returnDue == 0) returnDue = now + returnDelay.roll(); }
            else returnDue = 0;
            if (returnDue != 0 && now >= returnDue) {
                int s = sword(p); if (s >= 0) p.getInventory().setSelectedSlot(s);
                previousSlot = -1; returnDue = 0; hitDue = 0;
            }
            return;
        }
        if (!weaponSwitch.get() || !blocking || !CombatState.sword(p)) { switchDue = 0; return; }
        if (switchDue == 0) switchDue = now + switchDelay.roll();
        if (now < switchDue) return;
        int a = axe(p);
        if (a >= 0) { previousSlot = p.getInventory().getSelectedSlot(); p.getInventory().setSelectedSlot(a); hitDue = 0; }
        switchDue = 0;
    }
}
