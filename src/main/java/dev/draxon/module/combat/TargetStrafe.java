package dev.draxon.module.combat;

import dev.draxon.combat.*;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.BooleanSetting;
import dev.draxon.setting.ModeSetting;
import dev.draxon.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Orbits the target with the movement KEYS (no velocity changes): every tick a goal point on the circle, Speed degrees
 * ahead of the player, is converted into W/A/S/D relative to the current yaw.
 */
public class TargetStrafe extends Module {
    private final NumberSetting radius = add(new NumberSetting("Radius", 2.5, 1.0, 6.0, 0.1));
    private final NumberSetting speed = add(new NumberSetting("Speed", 30, 5, 90, 1).unit("\u00B0 lead"));
    private final ModeSetting direction = mode("Direction", "Clockwise", "Clockwise", "Counter-Clockwise");
    private final BooleanSetting dirSwitch = bool("Direction Switch", true), needForward = bool("Require Forward Key", true);
    private final NumberSetting targetRange = add(new NumberSetting("Target Range", 5.0, 1.0, 10.0, 0.5));
    private final NumberSetting correction = pct("Movement Correction", 60, 0, 100);

    private final TargetManager.Selector selector = new TargetManager.Selector();
    private boolean flipped, holding;

    public TargetStrafe() { super("TargetStrafe", Category.COMBAT, "Sword"); }
    @Override protected void onDisable() { release(MinecraftClient.getInstance()); selector.clear(); }
    @Override public void reset() { flipped = false; holding = false; selector.clear(); }
    private void release(MinecraftClient mc) { if (holding && mc.player != null) Keys.restoreMovement(mc); holding = false; }

    @Override public void onPreTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null || !CombatState.moveFree(this)) { release(mc); return; }
        if (needForward.get() && !Keys.physical(mc, mc.options.forwardKey)) { release(mc); return; }
        TargetManager.Req r = new TargetManager.Req(targetRange.get(), 360, false, true, true, TargetManager.Priority.DISTANCE, 0.5);
        LivingEntity t = selector.select(mc, p, r, 300);
        if (t == null) { release(mc); return; }

        if (dirSwitch.get() && p.horizontalCollision) flipped = !flipped;
        double dir = (direction.get().equals("Clockwise") ^ flipped) ? -1 : 1;
        Vec3d c = new Vec3d(t.getX(), t.getY(), t.getZ()), pp = new Vec3d(p.getX(), p.getY(), p.getZ());
        double dx = pp.x - c.x, dz = pp.z - c.z, dist = Math.hypot(dx, dz), k = correction.get() / 100.0;
        double rad = dist + (radius.get() - dist) * k;          // k=1 pulls onto the circle, k=0 keeps the current radius
        double ang = Math.atan2(dz, dx) + Math.toRadians(speed.get()) * dir;
        double gx = c.x + rad * Math.cos(ang) - pp.x, gz = c.z + rad * Math.sin(ang) - pp.z;
        float rel = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(gz, gx)) - 90.0) - p.getYaw());
        float a = Math.abs(rel);
        mc.options.forwardKey.setPressed(a < 67.5f);
        mc.options.backKey.setPressed(a > 112.5f);
        mc.options.rightKey.setPressed(rel > 22.5f && rel < 157.5f);
        mc.options.leftKey.setPressed(rel < -22.5f && rel > -157.5f);
        holding = true;
    }
}
