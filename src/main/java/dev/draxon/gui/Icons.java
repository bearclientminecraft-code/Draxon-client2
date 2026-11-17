package dev.draxon.gui;
import dev.draxon.module.Category;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;
/** Draws the supplied icon set from alpha masks (IconData) with plain fills; tintable, no textures. Rectangles are merged once at load. */
public final class Icons {
    public static final int SIZE = IconData.SIZE;
    private static final int[][][] RECTS = new int[Category.values().length][][]; // {x0, y0, x1, y1, alpha 1..15}
    static {
        for (Category c : Category.values()) {
            String[] m = IconData.MASKS.get(c.name()); List<int[]> out = new ArrayList<>();
            for (int r = 0; r < SIZE; r++) {
                int cx = 0;
                while (cx < SIZE) {
                    char ch = m[r].charAt(cx); int e = cx + 1;
                    while (e < SIZE && m[r].charAt(e) == ch) e++;
                    int a = Character.digit(ch, 16);
                    if (a > 0) {
                        boolean merged = false;
                        for (int[] q : out) if (q[3] == r && q[0] == cx && q[2] == e && q[4] == a) { q[3] = r + 1; merged = true; break; }
                        if (!merged) out.add(new int[]{cx, r, e, r + 1, a});
                    }
                    cx = e;
                }
            }
            RECTS[c.ordinal()] = out.toArray(new int[0][]);
        }
    }
    private Icons() {}
    public static void draw(DrawContext c, Category cat, int x, int y, int rgb, float alpha) {
        int last = -1, col = 0;
        for (int[] q : RECTS[cat.ordinal()]) {
            if (q[4] != last) { last = q[4]; col = ThemeManager.argb(rgb, alpha * q[4] / 15f); }
            c.fill(x + q[0], y + q[1], x + q[2], y + q[3], col);
        }
    }
}
