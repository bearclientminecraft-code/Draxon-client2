package dev.draxon.render;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.render.EspModule;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
/** 2D/3D/Corner/Outline ESP drawn in screen space from interpolated entity boxes. */
final class EspRenderer {
    private static final float[] hx = new float[10], hy = new float[10];
    private EspRenderer() {}
    static void draw(DrawContext c, MinecraftClient mc, LivingEntity e, float dist, float td, EspModule m) {
        int cat = EntityFilter.category(e); if (cat == EntityFilter.NONE) return;
        boolean friend = cat == EntityFilter.PLAYER && m.friends.get() && FriendManager.isFriend(e.getName().getString());
        boolean allowed = friend || switch (cat) { case EntityFilter.PLAYER -> m.players.get(); case EntityFilter.HOSTILE -> m.hostile.get(); case EntityFilter.PASSIVE -> m.passive.get(); default -> m.animals.get(); };
        if (!allowed || (m.visibility.get().equals("Visible Only") && !mc.player.canSee(e))) return;
        if (!BoundingBoxUtils.compute(e, td)) return;
        float ratio = Math.max(0f, Math.min(1f, e.getHealth() / Math.max(1f, e.getMaxHealth())));
        int rgb = friend ? m.friendColor.get() : m.healthColor.get() ? ColorManager.health(ratio) : switch (cat) { case EntityFilter.PLAYER -> m.playerColor.get(); case EntityFilter.HOSTILE -> m.hostileColor.get(); case EntityFilter.PASSIVE -> m.passiveColor.get(); default -> m.animalColor.get(); };
        float op = m.opacity.get().floatValue(), bg = m.bgOpacity.get().floatValue(); int t = m.thickness.get().intValue(), col = ThemeManager.argb(rgb, op);
        int x0 = Math.round(BoundingBoxUtils.minX), y0 = Math.round(BoundingBoxUtils.minY), x1 = Math.round(BoundingBoxUtils.maxX), y1 = Math.round(BoundingBoxUtils.maxY);
        switch (m.mode.get()) {
            case "3D Box" -> { for (int[] ed : BoundingBoxUtils.EDGES) RenderUtils.line(c, BoundingBoxUtils.px[ed[0]], BoundingBoxUtils.py[ed[0]], BoundingBoxUtils.px[ed[1]], BoundingBoxUtils.py[ed[1]], t, col); }
            case "Corner Box" -> {
                if (bg > 0) c.fill(x0, y0, x1, y1, ThemeManager.argb(rgb, bg));
                int l = Math.max(3, Math.min(x1 - x0, y1 - y0) / 4);
                c.fill(x0, y0, x0 + l, y0 + t, col); c.fill(x0, y0, x0 + t, y0 + l, col); c.fill(x1 - l, y0, x1, y0 + t, col); c.fill(x1 - t, y0, x1, y0 + l, col);
                c.fill(x0, y1 - t, x0 + l, y1, col); c.fill(x0, y1 - l, x0 + t, y1, col); c.fill(x1 - l, y1 - t, x1, y1, col); c.fill(x1 - t, y1 - l, x1, y1, col);
            }
            case "Outline" -> {
                int n = BoundingBoxUtils.hull();
                for (int i = 0; i < n; i++) { hx[i] = BoundingBoxUtils.px[BoundingBoxUtils.HULL[i]]; hy[i] = BoundingBoxUtils.py[BoundingBoxUtils.HULL[i]]; }
                if (bg > 0) RenderUtils.fillConvex(c, hx, hy, n, (int) ProjectionUtils.sh, ThemeManager.argb(rgb, bg));
                for (int pass = 2; pass >= 0; pass--) {
                    int th = t + pass * 2; int pc = pass == 0 ? col : ThemeManager.argb(rgb, op * (pass == 1 ? 0.22f : 0.10f));
                    for (int i = 0; i < n; i++) { int j = (i + 1) % n; RenderUtils.line(c, hx[i], hy[i], hx[j], hy[j], th, pc); }
                }
            }
            default -> { if (bg > 0) c.fill(x0, y0, x1, y1, ThemeManager.argb(rgb, bg)); RenderUtils.rectOutline(c, x0, y0, x1, y1, t, col); }
        }
        if (m.healthBar.get()) {
            int bx = x0 - 3 - t, bh = y1 - y0;
            c.fill(bx - 1, y0 - 1, bx + 2, y1 + 1, ThemeManager.argb(0x000000, 0.55f));
            c.fill(bx, y1 - (int) (bh * ratio), bx + 1, y1, ThemeManager.argb(ColorManager.health(ratio), 1f));
        }
        if (m.distance.get()) {
            String s = (int) dist + "m"; c.getMatrices().pushMatrix();
            c.getMatrices().translate((x0 + x1) / 2f, y1 + 2); c.getMatrices().scale(0.8f, 0.8f);
            FontManager.drawOutlined(c, s, -FontManager.width(s) / 2, 0, ThemeManager.argb(0xFFFFFF, 1f), true);
            c.getMatrices().popMatrix();
        }
    }
}
