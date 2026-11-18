package dev.draxon.module.player;

import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/** While you hold attack on a block, selects the hotbar tool that mines it fastest; optionally returns to the previous slot afterwards. */
public class AutoTool extends Module {
    private final BooleanSetting switchBack = bool("Switch Back", true), ignoreCreative = bool("Ignore Creative", true), saveDurability = bool("Avoid Nearly Broken Tools", true);
    private final NumberSetting backDelay = ms("Switch Back Delay", 150, 0, 1000);
    private int previous = -1, ours = -1; private long idleSince;

    public AutoTool() { super("AutoTool", Category.PLAYER); }
    @Override protected void onDisable() { MinecraftClient mc = MinecraftClient.getInstance(); if (mc.player != null) restore(mc.player); reset(); }
    @Override public void reset() { previous = ours = -1; idleSince = 0; }
    private void restore(ClientPlayerEntity p) {
        if (previous >= 0 && p.getInventory().getSelectedSlot() == ours) p.getInventory().setSelectedSlot(previous); // only if the player did not change slot meanwhile
        previous = ours = -1;
    }

    @Override public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null) return;
        boolean mining = mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK && !(ignoreCreative.get() && p.isCreative());
        if (!mining) {
            if (previous < 0) return;
            if (idleSince == 0) idleSince = System.currentTimeMillis();
            if (!switchBack.get()) { previous = ours = -1; idleSince = 0; }
            else if (System.currentTimeMillis() - idleSince >= backDelay.get().longValue()) { restore(p); idleSince = 0; }
            return;
        }
        idleSince = 0;
        BlockState state = mc.world.getBlockState(((BlockHitResult) mc.crosshairTarget).getBlockPos());
        if (state.isAir()) return;
        int cur = p.getInventory().getSelectedSlot(), best = cur; float bestSpeed = speed(p.getInventory().getStack(cur), state);
        for (int i = 0; i < 9; i++) {
            float s = speed(p.getInventory().getStack(i), state);
            if (s > bestSpeed + 0.01f) { bestSpeed = s; best = i; }
        }
        if (best != cur) { if (previous < 0) previous = cur; ours = best; p.getInventory().setSelectedSlot(best); }
    }

    private float speed(ItemStack st, BlockState state) {
        if (saveDurability.get() && st.isDamageable() && st.getMaxDamage() - st.getDamage() <= 2) return 1f; // treat as bare hand
        return st.getMiningSpeedMultiplier(state);
    }
}
