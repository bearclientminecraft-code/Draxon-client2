package dev.draxon.util;
import dev.draxon.gui.ThemeManager;
import net.minecraft.client.gui.DrawContext;
public class RenderUtils {
    /** Set once per frame by ThemeManager.sync(): small shapes (radius <= 5) use the hard-edged variant, panels keep anti-aliased corners. */
    public static boolean fast = true;

    /** Precomputed corner geometry per radius. Every GUI element is a handful of fills, so the number of fills (not pixels) is what costs time. */
    private static final class Shape {
        int[][] solid;   // {rowFrom, rowTo, firstFullColumn} per run of equal rows (anti-aliased shape, 4 coverage levels)
        int[][] aa;      // {col0, row0, col1, row1, level 1..3} merged partial-coverage rectangles of one corner
        int[][] ring;    // same for the 1px border ring (levels 1..4)
        int[][] hard;    // {rowFrom, rowTo, firstColumn} of the hard-edged (no AA) shape
    }
    private static final Shape[] SHAPES = new Shape[33];

    private static Shape shape(int r) {
        Shape sh = SHAPES[r]; if (sh != null) return sh;
        int[][] lv = new int[r][r], rv = new int[r][r]; int[] full = new int[r], half = new int[r];
        for (int i = 0; i < r; i++) {
            full[i] = r; half[i] = r;
            for (int j = 0; j < r; j++) {
                double d = Math.hypot(r - (j + 0.5), r - (i + 0.5));
                double outer = Math.max(0, Math.min(1, r - d + 0.5)), inner = Math.max(0, Math.min(1, (r - 1) - d + 0.5));
                lv[i][j] = (int) Math.round(outer * 4); rv[i][j] = (int) Math.round(Math.max(0, outer - inner) * 2); // the 1px ring only needs 2 levels
                if (lv[i][j] >= 4 && full[i] == r) full[i] = j;
                if (outer >= 0.5 && half[i] == r) half[i] = j;
            }
        }
        sh = new Shape();
        for (int i = 0; i < r; i++) for (int j = full[i]; j < r; j++) lv[i][j] = 0; // solid part is drawn by the row runs
        sh.solid = runs(full, r); sh.hard = runs(half, r);
        sh.aa = merge(lv, r, 3); sh.ring = merge(rv, r, 2);
        return SHAPES[r] = sh;
    }
    private static int[][] runs(int[] first, int r) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int i = 0; i < r; i++) {
            if (!out.isEmpty() && out.get(out.size() - 1)[2] == first[i] && out.get(out.size() - 1)[1] == i) out.get(out.size() - 1)[1] = i + 1;
            else out.add(new int[]{i, i + 1, first[i]});
        }
        return out.toArray(new int[0][]);
    }
    /** Merge equal-level pixels into rectangles: horizontal runs first, then identical runs of consecutive rows. */
    private static int[][] merge(int[][] lv, int r, int maxLevel) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int i = 0; i < r; i++) {
            int j = 0;
            while (j < r) {
                int l = lv[i][j]; if (l <= 0 || l > maxLevel) { j++; continue; }
                int e = j + 1; while (e < r && lv[i][e] == l) e++;
                boolean merged = false;
                for (int[] q : out) if (q[3] == i && q[0] == j && q[2] == e && q[4] == l) { q[3] = i + 1; merged = true; break; }
                if (!merged) out.add(new int[]{j, i, e, i + 1, l});
                j = e;
            }
        }
        return out.toArray(new int[0][]);
    }
    private static int scale(int color, float k) { return ((int) ((color >>> 24) * k) << 24) | (color & 0xFFFFFF); }
    private static int rad(int r, int w, int h) { return Math.min(Math.min(r, 32), Math.min(w, h) / 2); }

    /** Hard-edged rounded rect (no anti-aliasing): a middle block plus one fill per run of equal rows at each end. */
    public static void roundRectFast(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = rad(r, w, h);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        Shape sh = shape(r);
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int[] g : sh.hard) {
            c.fill(x + g[2], y + g[0], x + w - g[2], y + g[1], color);
            c.fill(x + g[2], y + h - g[1], x + w - g[2], y + h - g[0], color);
        }
    }
    /** Rounded rect with anti-aliased corners (4 coverage levels, merged into few rectangles). Corner radius is unchanged. */
    public static void roundRect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = rad(r, w, h);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        if (fast && r <= 5) { roundRectFast(c, x, y, w, h, r, color); return; }
        Shape sh = shape(r);
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int[] g : sh.solid) {
            c.fill(x + g[2], y + g[0], x + w - g[2], y + g[1], color);
            c.fill(x + g[2], y + h - g[1], x + w - g[2], y + h - g[0], color);
        }
        corners(c, x, y, w, h, sh.aa, color, 4);
    }
    private static void corners(DrawContext c, int x, int y, int w, int h, int[][] rects, int color, int levels) {
        int last = -1, col = 0;
        for (int[] q : rects) {
            if (q[4] != last) { last = q[4]; col = q[4] >= levels ? color : scale(color, q[4] / (float) levels); }
            c.fill(x + q[0], y + q[1], x + q[2], y + q[3], col);
            c.fill(x + w - q[2], y + q[1], x + w - q[0], y + q[3], col);
            c.fill(x + q[0], y + h - q[3], x + q[2], y + h - q[1], col);
            c.fill(x + w - q[2], y + h - q[3], x + w - q[0], y + h - q[1], col);
        }
    }
    /** 1px border that follows the rounded corners. */
    public static void border(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = rad(r, w, h);
        if (r <= 1) { border(c, x, y, w, h, color); return; }
        Shape sh = shape(r);
        c.fill(x + r, y, x + w - r, y + 1, color); c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color); c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        corners(c, x, y, w, h, sh.ring, color, 2);
    }
    /** Plain square-cornered 1px border. */
    public static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color); c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color); c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
    /** Soft drop shadow: three hard-edged layers (the old five anti-aliased layers cost ~600 fills per panel). */
    public static void shadow(DrawContext c, int x, int y, int w, int h, int r) {
        roundRectFast(c, x - 5, y - 2, w + 10, h + 10, r + 5, ThemeManager.argb(0x000000, 0.07f));
        roundRectFast(c, x - 3, y, w + 6, h + 6, r + 3, ThemeManager.argb(0x000000, 0.20f));
        roundRectFast(c, x - 1, y + 2, w + 2, h + 2, r + 1, ThemeManager.argb(0x000000, 0.30f));
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
