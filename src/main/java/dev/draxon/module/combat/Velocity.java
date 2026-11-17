package dev.draxon.module.combat;

import dev.draxon.combat.DelayManager;
import dev.draxon.core.DraxonClient;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Scales the knockback packet applied to the local player (hooked by ClientPlayNetworkHandlerMixin).
 * Effective horizontal factor = Horizontal * (Air or Ground Velocity); 100% everywhere = vanilla.
 */
public class Velocity extends Module {
    private final NumberSetting horizontal = pct("Horizontal", 100, 0, 100), vertical = pct("Vertical", 100, 0, 100),
            chance = pct("Chance", 100, 1, 100), airVel = pct("Air Velocity", 100, 0, 100), groundVel = pct("Ground Velocity", 100, 0, 100);
    private final NumberSetting delay = ms("Delay", 0, 0, 500);
    private final BooleanSetting dirCheck = bool("Direction Check", false);

    private final Random rng = new Random();
    private Vec3d queued; private long dueAt;

    public Velocity() { super("Velocity", Category.COMBAT, "Sword"); }
    @Override protected void onDisable() { reset(); }
    @Override public void reset() { queued = null; }

    /** Mixin hook. Returns the velocity to apply now, or null if it was queued (Delay) / must not be applied. */
    public static Vec3d handle(Entity e, Vec3d v) {
        Velocity m = DraxonClient.modules == null ? null : DraxonClient.modules.get(Velocity.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (m == null || !m.isEnabled() || mc.player == null || e != mc.player) return v;
        return m.modify(mc.player, v);
    }

    private Vec3d modify(ClientPlayerEntity p, Vec3d v) {
        if (rng.nextInt(100) >= chance.get().intValue()) return v;
        if (dirCheck.get()) { // only knockback that pushes the player away from where he is looking (hit from the front)
            Vec3d look = p.getRotationVec(1f);
            if (look.x * v.x + look.z * v.z >= 0) return v;
        }
        double h = horizontal.get() / 100.0 * (p.isOnGround() ? groundVel.get() : airVel.get()) / 100.0;
        Vec3d out = new Vec3d(v.x * h, v.y * vertical.get() / 100.0, v.z * h);
        long d = DelayManager.actual(delay.get(), 0);
        if (d <= 0) return out;
        queued = out; dueAt = System.currentTimeMillis() + d;
        return null;
    }

    @Override public void onTick(MinecraftClient mc) {
        if (queued != null && mc.player != null && System.currentTimeMillis() >= dueAt) { mc.player.setVelocity(queued); queued = null; }
    }
}
