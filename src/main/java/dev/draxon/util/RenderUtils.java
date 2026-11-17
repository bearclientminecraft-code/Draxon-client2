package dev.draxon.util;
import dev.draxon.gui.ThemeManager;
import net.minecraft.client.gui.DrawContext;
public class RenderUtils {
    private static final float[][][] COV = new float[33][][], BCOV = new float[33][][];
    private static final int[][] FULL = new int[33][];
    private static void ensure(int r) {
        if (COV[r] != null) return;
        float[][] f = new float[r][r], b = new float[r][r]; int[] first = new int[r];
        for (int i = 0; i < r; i++) {
            first[i] = r;
            for (int j = 0; j < r; j++) {
                double d = Math.hypot(r - (j + 0.5), r - (i + 0.5));
                float outer = (float) Math.max(0, Math.min(1, r - d + 0.5)), inner = (float) Math.max(0, Math.min(1, (r - 1) - d + 0.5));
                f[i][j] = outer; b[i][j] = Math.max(0, outer - inner);
                if (outer >= 0.999f && first[i] == r) first[i] = j;
            }
        }
        COV[r] = f; BCOV[r] = b; FULL[r] = first;
    }
    private static int scale(int color, float k) { return ((int) ((color >>> 24) * k) << 24) | (color & 0xFFFFFF); }
    /** Anti-aliased rounded rect: edge pixels get fractional alpha from analytic coverage. Tables are cached per radius; no per-frame allocation. */
    public static void roundRect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(Math.min(r, 32), Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        ensure(r); float[][] cv = COV[r]; int[] first = FULL[r];
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int jf = first[i];
            c.fill(x + jf, y + i, x + w - jf, y + i + 1, color);
            c.fill(x + jf, y + h - 1 - i, x + w - jf, y + h - i, color);
            for (int j = 0; j < jf; j++) {
                float k = cv[i][j]; if (k <= 0.01f) continue; int col = scale(color, k);
                c.fill(x + j, y + i, x + j + 1, y + i + 1, col); c.fill(x + w - 1 - j, y + i, x + w - j, y + i + 1, col);
                c.fill(x + j, y + h - 1 - i, x + j + 1, y + h - i, col); c.fill(x + w - 1 - j, y + h - 1 - i, x + w - j, y + h - i, col);
            }
        }
    }
    /** 1px border that follows the (anti-aliased) rounded corners. */
    public static void border(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(Math.min(r, 32), Math.min(w, h) / 2);
        if (r <= 1) { border(c, x, y, w, h, color); return; }
        ensure(r); float[][] bc = BCOV[r];
        c.fill(x + r, y, x + w - r, y + 1, color); c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color); c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) for (int j = 0; j < r; j++) {
            float k = bc[i][j]; if (k <= 0.02f) continue; int col = scale(color, k);
            c.fill(x + j, y + i, x + j + 1, y + i + 1, col); c.fill(x + w - 1 - j, y + i, x + w - j, y + i + 1, col);
            c.fill(x + j, y + h - 1 - i, x + j + 1, y + h - i, col); c.fill(x + w - 1 - j, y + h - 1 - i, x + w - j, y + h - i, col);
        }
    }
    /** Plain square-cornered 1px border. */
    public static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color); c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color); c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
    public static void shadow(DrawContext c, int x, int y, int w, int h, int r) {
        for (int i = 1; i <= 5; i++) roundRect(c, x - i, y - i + 3, w + 2 * i, h + 2 * i, r + i, ThemeManager.argb(0x000000, 0.045f * (6 - i)));
    }
    /** Frosted-glass sheen: soft light gradient from the top edge plus a bright hairline. Approximation, not a real blur. */
    public static void frost(DrawContext c, int x, int y, int w, int h, int r, float intensity, float alpha) {
        int inset = Math.max(2, r);
        c.fillGradient(x + inset, y + 1, x + w - inset, y + h / 2, ThemeManager.argb(0xFFFFFF, 0.07f * intensity * alpha), ThemeManager.argb(0xFFFFFF, 0f));
        c.fill(x + inset, y, x + w - inset, y + 1, ThemeManager.argb(0xFFFFFF, 0.16f * intensity * alpha));
    }
    public static void glassPanel(DrawContext c, int x, int y, int w, int h, ThemeManager t, float alpha) {
        shadow(c, x, y, w, h, t.rad());
        roundRect(c, x, y, w, h, t.rad(), ThemeManager.argb(t.panel, t.op() * alpha));
        frost(c, x, y, w, h, t.rad(), t.frost.get().floatValue(), alpha);
        border(c, x, y, w, h, t.rad(), t.border != 0 ? ThemeManager.argb(t.border, 0.55f * alpha) : ThemeManager.argb(0xFFFFFF, 0.08f * alpha));
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
        roundRect(c, cx - r, cy - r, 2 * r + 1, 2 * r + 1, r, color);
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
