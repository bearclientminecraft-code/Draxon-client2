package dev.draxon.render;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
/**
 * Software world-to-screen projection from the local player's interpolated eye pose.
 * Accurate in first person only (third-person camera offset and dynamic FOV effects are not modelled).
 * Output is in scaled GUI coordinates, i.e. the same space DrawContext uses.
 */
public final class ProjectionUtils {
    public static double camX, camY, camZ, tanHalf, aspect, sw, sh;
    private static double fx, fy, fz, rx, rz, ux, uy, uz;
    public static boolean ready;
    private ProjectionUtils() {}
    public static boolean update(MinecraftClient mc, float td) {
        ClientPlayerEntity p = mc.player;
        ready = false;
        if (p == null || mc.getWindow() == null || !mc.options.getPerspective().isFirstPerson()) return false;
        Vec3d pos = p.getLerpedPos(td);
        camX = pos.x; camY = pos.y + (p.getEyeY() - p.getY()); camZ = pos.z;
        double yaw = Math.toRadians(p.getYaw(td)), pitch = Math.toRadians(p.getPitch(td));
        double cp = Math.cos(pitch), sp = Math.sin(pitch), cyw = Math.cos(yaw), syw = Math.sin(yaw);
        fx = -syw * cp; fy = -sp; fz = cyw * cp;
        rx = -cyw; rz = -syw;
        // up = right x forward (right.y = 0)
        ux = (0 * fz) - (rz * fy); uy = (rz * fx) - (rx * fz); uz = (rx * fy) - (0 * fx);
        tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        sw = mc.getWindow().getScaledWidth(); sh = mc.getWindow().getScaledHeight(); aspect = sw / sh;
        ready = true;
        return true;
    }
    /** Pixels per world unit at depth 1 (used for sizing). */
    public static double focalPx() { return (sh / 2.0) / tanHalf; }
    /** Writes screen x,y,depth into out; returns false if the point is behind the camera. */
    public static boolean project(double x, double y, double z, double[] out) {
        double dx = x - camX, dy = y - camY, dz = z - camZ;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.05) return false;
        double px = dx * rx + dz * rz, py = dx * ux + dy * uy + dz * uz;
        out[0] = (px / (depth * tanHalf * aspect) * 0.5 + 0.5) * sw;
        out[1] = (0.5 - py / (depth * tanHalf) * 0.5) * sh;
        out[2] = depth;
        return true;
    }
}
