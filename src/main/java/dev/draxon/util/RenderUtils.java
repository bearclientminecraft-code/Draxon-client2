package dev.draxon.util;
import dev.draxon.gui.ThemeManager;
import net.minecraft.client.gui.DrawContext;
public class RenderUtils {
    /** Allocation-free rounded rect built from horizontal strips. */
    public static void roundRect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5; int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        c.fill(x, y + r, x + w, y + h - r, color);
    }
    public static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 1, y, x + w - 1, y + 1, color); c.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color); c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
    public static void shadow(DrawContext c, int x, int y, int w, int h, int r) {
        for (int i = 1; i <= 5; i++) roundRect(c, x - i, y - i + 3, w + 2 * i, h + 2 * i, r + i, ThemeManager.argb(0x000000, 0.045f * (6 - i)));
    }
    /** Frosted-glass sheen: soft light gradient from the top edge plus a bright hairline. Approximation, not a real blur. */
    public static void frost(DrawContext c, int x, int y, int w, int h, int r, float intensity, float alpha) {
        int inset = Math.max(2, r / 2);
        c.fillGradient(x + inset, y + 1, x + w - inset, y + h / 2, ThemeManager.argb(0xFFFFFF, 0.07f * intensity * alpha), ThemeManager.argb(0xFFFFFF, 0f));
        c.fill(x + inset, y, x + w - inset, y + 1, ThemeManager.argb(0xFFFFFF, 0.16f * intensity * alpha));
    }
    public static void glassPanel(DrawContext c, int x, int y, int w, int h, ThemeManager t, float alpha) {
        shadow(c, x, y, w, h, t.rad());
        roundRect(c, x, y, w, h, t.rad(), ThemeManager.argb(t.panel, t.op() * alpha));
        frost(c, x, y, w, h, t.rad(), t.frost.get().floatValue(), alpha);
        border(c, x, y, w, h, ThemeManager.argb(0xFFFFFF, 0.08f * alpha));
    }

    public static void rectOutline(DrawContext c, int x0, int y0, int x1, int y1, int t, int color) {
        c.fill(x0, y0, x1, y0 + t, color); c.fill(x0, y1 - t, x1, y1, color);
        c.fill(x0, y0 + t, x0 + t, y1 - t, color); c.fill(x1 - t, y0 + t, x1, y1 - t, color);
    }
    /** Thick line drawn as horizontal/vertical runs: about min(|dx|,|dy|)+1 fills instead of one fill per pixel. */
    public static void line(DrawContext c, float x0, float y0, float x1, float y1, int t, int color) {
        float dx = x1 - x0, dy = y1 - y0, hl = t / 2f;
        if (Math.abs(dx) < 0.5f) { c.fill(Math.round(x0 - hl), Math.round(Math.min(y0, y1) - hl), Math.round(x0 - hl) + t, Math.round(Math.max(y0, y1) + hl), color); return; }
        if (Math.abs(dy) < 0.5f) { c.fill(Math.round(Math.min(x0, x1) - hl), Math.round(y0 - hl), Math.round(Math.max(x0, x1) + hl), Math.round(y0 - hl) + t, color); return; }
        boolean shallow = Math.abs(dx) >= Math.abs(dy);
        int n = (int) Math.min(2000, Math.ceil(Math.min(Math.abs(dx), Math.abs(dy)))) + 1;
        for (int i = 0; i < n; i++) {
            float a = i / (float) n, b = (i + 1) / (float) n;
            float xa = x0 + dx * a, xb = x0 + dx * b, ya = y0 + dy * a, yb = y0 + dy * b;
            if (shallow) c.fill(Math.round(Math.min(xa, xb) - hl), Math.round(ya - hl), Math.round(Math.max(xa, xb) + hl), Math.round(ya - hl) + t, color);
            else c.fill(Math.round(xa - hl), Math.round(Math.min(ya, yb) - hl), Math.round(xa - hl) + t, Math.round(Math.max(ya, yb) + hl), color);
        }
    }
    public static void disc(DrawContext c, int cx, int cy, int r, int color) {
        if (r <= 0) { c.fill(cx, cy, cx + 1, cy + 1, color); return; }
        for (int dy = -r; dy <= r; dy++) { int h = (int) Math.round(Math.sqrt(r * r - dy * dy)); c.fill(cx - h, cy + dy, cx + h + 1, cy + dy + 1, color); }
    }
    public static void ring(DrawContext c, int cx, int cy, int r, int color) {
        int ri = Math.max(0, r - 1);
        for (int dy = -r; dy <= r; dy++) {
            int ho = (int) Math.round(Math.sqrt(r * r - dy * dy));
            if (Math.abs(dy) >= ri) c.fill(cx - ho, cy + dy, cx + ho + 1, cy + dy + 1, color);
            else { int hi = (int) Math.round(Math.sqrt(ri * ri - dy * dy)); c.fill(cx - ho, cy + dy, cx - hi, cy + dy + 1, color); c.fill(cx + hi + 1, cy + dy, cx + ho + 1, cy + dy + 1, color); }
        }
    }
    /** Scanline fill of a convex polygon (screen-clipped). */
    public static void fillConvex(DrawContext c, float[] xs, float[] ys, int n, int sh, int color) {
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) { minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]); }
        for (int y = Math.max(0, (int) Math.floor(minY)); y <= Math.min(sh, (int) Math.ceil(maxY)); y++) {
            float yy = y + 0.5f, xl = Float.MAX_VALUE, xr = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n; float yi = ys[i], yj = ys[j];
                if ((yy >= yi && yy < yj) || (yy >= yj && yy < yi)) { float x = xs[i] + (yy - yi) * (xs[j] - xs[i]) / (yj - yi); xl = Math.min(xl, x); xr = Math.max(xr, x); }
            }
            if (xl < xr) c.fill(Math.round(xl), y, Math.round(xr), y + 1, color);
        }
    }
}
