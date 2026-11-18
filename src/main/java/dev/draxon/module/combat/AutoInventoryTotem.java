package dev.draxon.module.combat;

import dev.draxon.combat.DelayManager;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * When the player inventory (E) opens, waits Delay +- Random Delay and swaps a totem from the main inventory into the chosen
 * hotbar slot (SWAP click with that hotbar key). Does nothing if that slot already holds a totem. Once per opening.
 */
public class AutoInventoryTotem extends Module {
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot", 9, 1, 9, 1));
    private final DelayManager.Delay delay = delay("Delay", 100, 1000, "Random Delay", 10, 200);
    private boolean wasOpen, done; private long due;

    public AutoInventoryTotem() { super("AutoInventoryTotem", Category.COMBAT, "Crystal"); }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { wasOpen = done = false; due = 0; }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        boolean open = p != null && mc.currentScreen instanceof InventoryScreen;
        if (!open) { wasOpen = done = false; return; }
        long now = System.currentTimeMillis();
        if (!wasOpen) { wasOpen = true; done = false; due = now + delay.roll(); }
        if (done || now < due || mc.interactionManager == null || p.currentScreenHandler == null) return;
        done = true;
        int hot = totemSlot.get().intValue() - 1;
        if (p.getInventory().getStack(hot).isOf(Items.TOTEM_OF_UNDYING)) return;
        for (int i = 9; i < 36; i++) { // main inventory: inventory index == slot id in the player screen handler
            if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                mc.interactionManager.clickSlot(p.currentScreenHandler.syncId, i, hot, SlotActionType.SWAP, p);
                return;
            }
        }
    }
}
