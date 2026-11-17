package dev.draxon.render;
import dev.draxon.core.DraxonClient;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.render.ParticlesModule;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import java.util.Random;
/** Pooled ambient particles simulated in world space and drawn through ProjectionUtils. Structure-of-arrays, zero per-frame allocation. */
public final class ParticleManager {
    private static final int MAX = 400, T_GLOW = 0, T_DUST = 1, T_SOFT = 2, T_SPARK = 3, T_CIRCLE = 4, T_FLOAT = 5;
    private static final double[] wx = new double[MAX], wy = new double[MAX], wz = new double[MAX];
    private static final float[] vx = new float[MAX], vy = new float[MAX], vz = new float[MAX], age = new float[MAX], life = new float[MAX], size = new float[MAX], hue = new float[MAX], phase = new float[MAX];
    private static final byte[] type = new byte[MAX]; private static final boolean[] alive = new boolean[MAX];
    private static int aliveCount, cursor; private static float spawnAcc, time;
    private static final Random rnd = new Random(); private static final double[] out = new double[3];
    private ParticleManager() {}
    public static void clear() { java.util.Arrays.fill(alive, false); aliveCount = 0; }

    public static void update(float dt, ParticlesModule m) {
        time += dt;
        boolean perf = m.performance.get(); int cap = Math.min(MAX, (int) (m.count.get() * (perf ? 0.4 : 1.0)));
        float R = m.distance.get().floatValue(), spd = m.speed.get().floatValue(), lt = m.lifetime.get().floatValue();
        spawnAcc += dt * (cap / lt) * m.density.get().floatValue();
        while (spawnAcc >= 1f && aliveCount < cap) { spawnAcc -= 1f; spawn(m, R, spd, lt); }
        if (aliveCount >= cap) spawnAcc = Math.min(spawnAcc, 1f);
        boolean orbit = m.aroundCamera.get(); float omega = 0.12f * spd;
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) continue;
            age[i] += dt;
            double dx = wx[i] - ProjectionUtils.camX, dz = wz[i] - ProjectionUtils.camZ;
            if (age[i] >= life[i] || dx * dx + dz * dz > R * R * 1.7 || Math.abs(wy[i] - ProjectionUtils.camY) > R) { alive[i] = false; aliveCount--; continue; }
            float wob = (float) Math.sin(time * 0.9 + phase[i]) * 0.25f * spd;
            if (orbit) { double ang = omega * dt, cs = Math.cos(ang), sn = Math.sin(ang); wx[i] = ProjectionUtils.camX + dx * cs - dz * sn; wz[i] = ProjectionUtils.camZ + dx * sn + dz * cs; }
            wx[i] += (vx[i] + wob) * dt; wy[i] += (vy[i] + (float) Math.cos(time * 0.7 + phase[i]) * 0.1f * spd) * dt; wz[i] += (vz[i] - wob) * dt;
        }
    }
    private static void spawn(ParticlesModule m, float R, float spd, float lt) {
        int slot = -1; for (int k = 0; k < MAX; k++) { int i = (cursor + k) % MAX; if (!alive[i]) { slot = i; cursor = i + 1; break; } }
        if (slot < 0) return;
        wx[slot] = ProjectionUtils.camX + (rnd.nextFloat() - 0.5f) * 2 * R; wy[slot] = ProjectionUtils.camY + (rnd.nextFloat() - 0.4f) * R * 0.9; wz[slot] = ProjectionUtils.camZ + (rnd.nextFloat() - 0.5f) * 2 * R;
        vx[slot] = (rnd.nextFloat() - 0.5f) * 0.4f * spd; vy[slot] = (rnd.nextFloat() - 0.3f) * 0.25f * spd; vz[slot] = (rnd.nextFloat() - 0.5f) * 0.4f * spd;
        age[slot] = 0; life[slot] = lt * (0.6f + rnd.nextFloat() * 0.8f); size[slot] = 0.6f + rnd.nextFloat() * 0.8f; hue[slot] = rnd.nextFloat(); phase[slot] = rnd.nextFloat() * 6.283f;
        String ty = m.type.get(); type[slot] = (byte) (switch (ty) { case "Glow" -> T_GLOW; case "Dust" -> T_DUST; case "Soft Light" -> T_SOFT; case "Sparkle" -> T_SPARK; case "Circle" -> T_CIRCLE; case "Floating" -> T_FLOAT; default -> rnd.nextInt(6); });
        alive[slot] = true; aliveCount++;
    }

    public static void render(DrawContext c, ParticlesModule m) {
        boolean perf = m.performance.get(), glow = m.glow.get() && !perf; float R = m.distance.get().floatValue(), op = m.opacity.get().floatValue(), base = m.size.get().floatValue();
        double focal = ProjectionUtils.focalPx(); String cm = m.colorMode.get(); int custom = cm.equals("Theme") ? DraxonClient.theme.acc() : m.color.get();
        for (int i = 0; i < MAX; i++) {
            if (!alive[i] || !ProjectionUtils.project(wx[i], wy[i], wz[i], out)) continue;
            int sx = (int) out[0], sy = (int) out[1]; if (sx < -30 || sy < -30 || sx > ProjectionUtils.sw + 30 || sy > ProjectionUtils.sh + 30) continue;
            float a = Math.min(Math.min(age[i] / 0.8f, (life[i] - age[i]) / 1.0f), 1f) * op * (float) Math.max(0, 1 - out[2] / R);
            if (a < 0.02f) continue;
            int r = (int) Math.max(1, Math.min(20, base * size[i] * 0.03 * focal / out[2] + 0.5));
            int rgb = cm.equals("Rainbow") ? ColorManager.rainbow(time * 0.08f, hue[i], 0.6f) : custom;
            switch (type[i]) {
                case T_GLOW -> { if (glow) { RenderUtils.disc(c, sx, sy, r * 3, ThemeManager.argb(rgb, a * 0.08f)); RenderUtils.disc(c, sx, sy, r * 2, ThemeManager.argb(rgb, a * 0.16f)); } RenderUtils.disc(c, sx, sy, r, ThemeManager.argb(rgb, a)); }
                case T_DUST -> { int d = Math.max(1, r / 2); c.fill(sx, sy, sx + d, sy + d, ThemeManager.argb(0xFFFFFF, a * 0.6f)); }
                case T_SOFT -> { int rr = Math.min(20, r * 3); RenderUtils.disc(c, sx, sy, rr, ThemeManager.argb(rgb, a * 0.05f)); RenderUtils.disc(c, sx, sy, rr * 2 / 3, ThemeManager.argb(rgb, a * 0.07f)); RenderUtils.disc(c, sx, sy, rr / 3, ThemeManager.argb(rgb, a * 0.10f)); }
                case T_SPARK -> { float tw = 0.4f + 0.6f * Math.abs((float) Math.sin(time * 5 + phase[i])); int len = Math.max(2, r + 1), col = ThemeManager.argb(rgb, a * tw);
                    c.fill(sx - len, sy, sx + len + 1, sy + 1, col); c.fill(sx, sy - len, sx + 1, sy + len + 1, col); c.fill(sx, sy, sx + 1, sy + 1, ThemeManager.argb(0xFFFFFF, a * tw)); }
                case T_CIRCLE -> RenderUtils.ring(c, sx, sy, Math.max(2, r + 1), ThemeManager.argb(rgb, a * 0.8f));
                default -> RenderUtils.disc(c, sx, sy, Math.max(0, r - 1), ThemeManager.argb(rgb, a * 0.7f));
            }
        }
    }
}
