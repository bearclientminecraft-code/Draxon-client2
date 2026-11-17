package dev.draxon.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class Geo {
    private Geo() {}

    /** Nearest point of the entity's hitbox to the player's eyes. */
    public static Vec3d closest(ClientPlayerEntity p, Entity e) {
        Vec3d eye = p.getEyePos(); Box b = e.getBoundingBox();
        return new Vec3d(MathHelper.clamp(eye.x, b.minX, b.maxX), MathHelper.clamp(eye.y, b.minY, b.maxY), MathHelper.clamp(eye.z, b.minZ, b.maxZ));
    }
    public static double eyeDistance(ClientPlayerEntity p, Entity e) { return p.getEyePos().distanceTo(closest(p, e)); }

    /** Angle in degrees between the look vector and the direction to a point. */
    public static double angleTo(ClientPlayerEntity p, Vec3d point) {
        Vec3d look = p.getRotationVec(1f).normalize();
        Vec3d to = point.subtract(p.getEyePos()).normalize();
        return Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(to), -1.0, 1.0)));
    }
    public static float yawTo(Vec3d from, Vec3d to) { return (float) (Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0); }
    public static float pitchTo(Vec3d from, Vec3d to) {
        return (float) -Math.toDegrees(Math.atan2(to.y - from.y, Math.hypot(to.x - from.x, to.z - from.z)));
    }
    /** True when no block collider lies between the eyes and the nearest hitbox point. */
    public static boolean visible(MinecraftClient mc, ClientPlayerEntity p, Entity e) {
        BlockHitResult r = mc.world.raycast(new RaycastContext(p.getEyePos(), closest(p, e),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        return r.getType() == HitResult.Type.MISS;
    }
}
