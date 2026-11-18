package dev.draxon.hud;
import dev.draxon.gui.AnimationManager.Anim;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.Glyphs;
import dev.draxon.gui.ThemeManager;
import dev.draxon.setting.NumberSetting;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Glass list panel: icon + title header, accent separator, rows (swatch, label, right-aligned value).
 * Appears with fade + slide (from the nearest screen edge) + slight zoom, grows/shrinks smoothly, new rows fade in.
 * Rows are staged by add() and shown by commit(), so the old content stays on screen while the panel fades out.
 */
final class ListPanel {
    static final int MAXR = 10, ROW_H = 14, HEAD = 17;
    final String title; final int glyph; NumberSetting xs, ys;
    private final String[] nl = new String[MAXR], nr = new String[MAXR]; private final int[] nc = new int[MAXR]; private final boolean[] nd = new boolean[MAXR]; private int ncount;
    private final String[] left = new String[MAXR], right = new String[MAXR]; private final int[] colors = new int[MAXR]; private final boolean[] dim = new boolean[MAXR]; private int count;
    private final Anim vis = new Anim(0), hgt = new Anim(0); private final Map<String, Anim> rowAnim = new HashMap<>();
    private int cw, ch; private boolean dirty = true;
    int rx, ry, rw, rh; // last drawn rect in GUI px (used for dragging)
    ListPanel(String title, int glyph) { this.title = title; this.glyph = glyph; }

    void clear() { ncount = 0; }
    void add(String l, String r, int color, boolean dimmed) { if (ncount < MAXR) { nl[ncount] = l; nr[ncount] = r; nc[ncount] = color; nd[ncount] = dimmed; ncount++; } }
    int pending() { return ncount; }
    /** Publishes the staged rows. With no staged rows the old ones stay so the panel can fade out. */
    void commit() {
        if (ncount == 0) return;
        Set<String> keep = new HashSet<>();
        for (int i = 0; i < ncount; i++) { left[i] = nl[i]; right[i] = nr[i]; colors[i] = nc[i]; dim[i] = nd[i]; keep.add(nl[i]); }
        count = ncount; dirty = true; rowAnim.keySet().retainAll(keep);
    }

    void draw(DrawContext c, ThemeManager t, float scale, int sw, int sh, boolean chatOpen, float dt, boolean enabled) {
        vis.target = enabled && ncount > 0 ? 1f : 0f; vis.update(dt, 12f);
        if (vis.value <= 0.01f || count == 0) { rw = 0; return; }
        if (dirty) {
            dirty = false; int m = FontManager.width(title) + 30;
            for (int i = 0; i < count; i++) m = Math.max(m, 7 + 10 + FontManager.width(left[i]) + 14 + FontManager.width(right[i]) + 7);
            cw = Math.max(m, 110); ch = HEAD + count * ROW_H + 5;
        }
        if (hgt.value <= 0f) hgt.value = ch; // first appearance: no growing from zero height
        hgt.target = ch; hgt.update(dt, 14f);
        int w = cw, h = Math.round(hgt.value);
        float a = vis.value, ease = 1f - (1f - a) * (1f - a);
        int px = (int) Math.max(0, Math.min(sw - w * scale, xs.get() * sw)), py = (int) Math.max(0, Math.min(sh - ch * scale, ys.get() * sh));
        float slide = (1f - ease) * 16f * (px + w * scale / 2f < sw / 2f ? -1f : 1f), zoom = scale * (0.94f + 0.06f * ease);
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(px + w * scale / 2f + slide, py + h * scale / 2f);
        c.getMatrices().scale(zoom, zoom);
        c.getMatrices().translate(-w / 2f, -h / 2f);
        RenderUtils.glassPanel(c, 0, 0, w, h, t, a);
        Glyphs.draw(c, glyph, 7, 5, t.flower, a);
        FontManager.draw(c, title, 18, 4, ThemeManager.argb(t.text, a));
        c.fill(5, HEAD - 2, w - 5, HEAD - 1, ThemeManager.argb(t.acc(), 0.35f * a));
        for (int i = 0; i < count; i++) {
            int y0 = HEAD + i * ROW_H;
            if (y0 + ROW_H > h + 1) break; // the panel is still growing: the row appears once it fits
            Anim ra = rowAnim.computeIfAbsent(left[i], k -> new Anim(0f)); ra.target = 1f; ra.update(dt, 10f);
            float ar = a * ra.value; int ox = Math.round((1f - ra.value) * 8f);
            RenderUtils.roundRect(c, 7 + ox, y0 + 3, 6, 6, 2, ThemeManager.argb(colors[i], ar));
            FontManager.draw(c, left[i], 17 + ox, y0 + 1, ThemeManager.argb(t.text, ar));
            FontManager.draw(c, right[i], w - 7 - FontManager.width(right[i]) + ox, y0 + 1, ThemeManager.argb(dim[i] ? t.disabled : t.textDim, ar));
        }
        c.getMatrices().popMatrix();
        rx = px; ry = py; rw = (int) (w * scale); rh = (int) (ch * scale);
        if (chatOpen) RenderUtils.border(c, rx - 1, ry - 1, rw + 2, rh + 2, t.rad(), ThemeManager.argb(t.acc(), 0.55f * a));
    }
    boolean contains(int mx, int my) { return rw > 0 && mx >= rx && mx < rx + rw && my >= ry && my < ry + rh; }
}
