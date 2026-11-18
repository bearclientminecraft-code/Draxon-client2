package dev.draxon.module.combat;

import dev.draxon.combat.DelayManager;
import dev.draxon.combat.Keys;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.KeybindSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Respawn anchor macro. One press of the Activate Key runs: Anchor slot + use -> Glowstone slot + use -> Totem slot + use.
 * Every step selects its hotbar slot, waits Delay +- Random Delay, then queues one vanilla use-click (same path as AutoClicker).
 * Steps 2 and 3 need the crosshair on a respawn anchor (a freshly placed anchor is between you and the block you aimed at).
 */
public class AnchorMacro extends Module {
    private final KeybindSetting activate = add(new KeybindSetting("Activate Key", -1));
    private final NumberSetting anchorSlot = add(new NumberSetting("Anchor Slot", 1, 1, 9, 1));
    private final NumberSetting glowSlot = add(new NumberSetting("GlowStone Slot", 2, 1, 9, 1));
    private final NumberSetting totemSlot = add(new NumberSetting("Totem Slot", 3, 1, 9, 1));
    private final DelayManager.Delay delay = delay("Delay", 50, 500, "Random Delay", 5, 100);
    private final BooleanSetting placeAnchor = bool("Place Anchor", true), switchBack = bool("Switch Back", true);

    private static final long STEP_TIMEOUT_MS = 1500;
    private int step = -1, phase, prevSlot = -1, slot; // step: 0 anchor, 1 glowstone, 2 totem
    private long due, startedAt; private boolean keyWas;

    public AnchorMacro() { super("AnchorMacro", Category.COMBAT, "Crystal"); }
    @Override protected void onDisable() { finish(MinecraftClient.getInstance()); }
    @Override public void reset() { step = -1; phase = 0; prevSlot = -1; keyWas = false; }
    @Override public void validate() { reset(); }

    private NumberSetting slotSetting(int s) { return s == 0 ? anchorSlot : s == 1 ? glowSlot : totemSlot; }
    private static Item item(int s) { return s == 0 ? Items.RESPAWN_ANCHOR : s == 1 ? Items.GLOWSTONE : Items.TOTEM_OF_UNDYING; }

    private boolean keyDown(MinecraftClient mc) {
        int k = activate.get();
        return k >= 0 && mc.getWindow() != null && GLFW.glfwGetKey(mc.getWindow().getHandle(), k) == GLFW.GLFW_PRESS;
    }
    private static boolean onAnchor(MinecraftClient mc) {
        return mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK
                && mc.world.getBlockState(b.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
    }
    private static boolean onBlock(MinecraftClient mc) {
        return mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK;
    }

    private void finish(MinecraftClient mc) {
        if (mc != null && mc.player != null && switchBack.get() && prevSlot >= 0) mc.player.getInventory().setSelectedSlot(prevSlot);
        step = -1; phase = 0; prevSlot = -1;
    }
    /** Selects the step's slot (aborts if it does not hold the expected item) and schedules the click. */
    private void begin(MinecraftClient mc, ClientPlayerEntity p, long now) {
        slot = slotSetting(step).get().intValue() - 1;
        if (!p.getInventory().getStack(slot).isOf(item(step))) { finish(mc); return; }
        p.getInventory().setSelectedSlot(slot);
        startedAt = now; due = now + delay.roll(); phase = 1;
    }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) { if (step >= 0) finish(mc); keyWas = false; return; }
        boolean down = keyDown(mc), edge = down && !keyWas; keyWas = down;
        long now = System.currentTimeMillis();
        if (step < 0) {
            if (!edge) return;
            prevSlot = p.getInventory().getSelectedSlot();
            step = placeAnchor.get() ? 0 : 1;
            begin(mc, p, now);
            return;
        }
        if (phase == 2) { // the click queued last tick has been processed by the game: safe to change slot now
            step++;
            if (step > 2) { finish(mc); return; }
            begin(mc, p, now);
            return;
        }
        if (p.getInventory().getSelectedSlot() != slot) { finish(mc); return; } // the player changed slot: abort
        if (now < due) return;
        boolean aimed = step == 0 ? onBlock(mc) : onAnchor(mc);
        if (!aimed) { if (now - startedAt > STEP_TIMEOUT_MS) finish(mc); return; }
        Keys.click(mc.options.useKey);
        phase = 2;
    }
}
