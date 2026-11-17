package dev.draxon.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumpingCooldown") int draxon$getJumpingCooldown();
    @Accessor("jumpingCooldown") void draxon$setJumpingCooldown(int ticks);
}
