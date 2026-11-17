package dev.draxon.module.movement;

import dev.draxon.mixin.LivingEntityAccessor;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

/** Caps vanilla's jump cooldown (10 ticks) so holding jump re-jumps as soon as you land. 0 = no delay. */
public class NoJumpDelay extends Module {
    private final NumberSetting delay = add(new NumberSetting("Jump Delay", 0, 0, 10, 1).unit(" ticks"));

    public NoJumpDelay() { super("NoJumpDelay", Category.MOVEMENT); }

    @Override public void onPreTick(MinecraftClient mc) {
        if (mc.player == null) return;
        LivingEntityAccessor a = (LivingEntityAccessor) mc.player;
        if (a.draxon$getJumpingCooldown() > delay.get().intValue()) a.draxon$setJumpingCooldown(delay.get().intValue());
    }
}
