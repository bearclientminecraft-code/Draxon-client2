package dev.draxon.hud;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.Glyphs;
import dev.draxon.gui.ThemeManager;
import dev.draxon.setting.NumberSetting;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
/** Glass list panel in the reference style: icon + title header, accent separator, rows (swatch, label, right-aligned value). */
final class ListPanel {
    static final int MAXR = 10, ROW_H = 14, HEAD = 17;
    final String title; final int glyph; NumberSetting xs, ys;
    final String[] left = new String[MAXR], right = new String[MAXR]; final int[] colors = new int[MAXR]; final boolean[] dim = new boolean[MAXR]; int count;
    int rx, ry, rw, rh; // last drawn rect in GUI px (used for dragging)
    ListPanel(String title, int glyph) { this.title = title; this.glyph = glyph; }
    void clear() { count = 0; }
    void add(String l, String r, int color, boolean dimmed) { if (count < MAXR) { left[count] = l; right[count] = r; colors[count] = color; dim[count] = dimmed; count++; } }

    void draw(DrawContext c, ThemeManager t, float scale, int sw, int sh, boolean chatOpen) {
        if (count == 0 && !chatOpen) { rw = 0; return; }
        int w = FontManager.width(title) + 30;
        for (int i = 0; i < count; i++) w = Math.max(w, 7 + 10 + FontManager.width(left[i]) + 14 + FontManager.width(right[i]) + 7);
        w = Math.max(w, 110); int h = HEAD + count * ROW_H + 5;
        int px = (int) Math.max(0, Math.min(sw - w * scale, xs.get() * sw)), py = (int) Math.max(0, Math.min(sh - h * scale, ys.get() * sh));
        c.getMatrices().pushMatrix(); c.getMatrices().translate(px, py); c.getMatrices().scale(scale, scale);
        RenderUtils.glassPanel(c, 0, 0, w, h, t, 1f);
        Glyphs.draw(c, glyph, 7, 5, t.flower, 1f);
        FontManager.draw(c, title, 18, 4, ThemeManager.argb(t.text, 1f));
        c.fill(5, HEAD - 2, w - 5, HEAD - 1, ThemeManager.argb(t.acc(), 0.35f));
        for (int i = 0; i < count; i++) {
            int y0 = HEAD + i * ROW_H;
            RenderUtils.roundRect(c, 7, y0 + 3, 6, 6, 2, ThemeManager.argb(colors[i], 1f));
            FontManager.draw(c, left[i], 17, y0 + 1, ThemeManager.argb(t.text, 1f));
            FontManager.draw(c, right[i], w - 7 - FontManager.width(right[i]), y0 + 1, ThemeManager.argb(dim[i] ? t.disabled : t.textDim, 1f));
        }
        c.getMatrices().popMatrix();
        rx = px; ry = py; rw = (int) (w * scale); rh = (int) (h * scale);
        if (chatOpen) RenderUtils.border(c, rx - 1, ry - 1, rw + 2, rh + 2, t.rad(), ThemeManager.argb(t.acc(), 0.55f));
    }
    boolean contains(int mx, int my) { return rw > 0 && mx >= rx && mx < rx + rw && my >= ry && my < ry + rh; }
}
