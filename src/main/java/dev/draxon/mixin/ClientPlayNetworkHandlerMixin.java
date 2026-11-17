package dev.draxon.mixin;

import dev.draxon.module.combat.Velocity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    /** Knockback packet: let the Velocity module scale (or delay) the velocity applied to the local player. */
    @Redirect(method = "onEntityVelocityUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocityClient(Lnet/minecraft/util/math/Vec3d;)V"))
    private void draxon$velocity(Entity entity, Vec3d velocity) {
        Vec3d out = Velocity.handle(entity, velocity);
        if (out != null) entity.setVelocityClient(out);
    }
}
