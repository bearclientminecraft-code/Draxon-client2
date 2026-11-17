package dev.draxon.gui;
import dev.draxon.module.Category;
import net.minecraft.client.gui.DrawContext;
/** Draws the supplied icon set from alpha masks (IconData) with plain fills; tintable, no textures. */
public final class Icons {
    public static final int SIZE = IconData.SIZE;
    private static final String[][] MASKS = new String[Category.values().length][];
    static { for (Category c : Category.values()) MASKS[c.ordinal()] = IconData.MASKS.get(c.name()); }
    private Icons() {}
    public static void draw(DrawContext c, Category cat, int x, int y, int rgb, float alpha) {
        String[] m = MASKS[cat.ordinal()];
        for (int r = 0; r < SIZE; r++) {
            String row = m[r]; int cx = 0;
            while (cx < SIZE) {
                char ch = row.charAt(cx); int e = cx + 1;
                while (e < SIZE && row.charAt(e) == ch) e++;
                int a = Character.digit(ch, 16);
                if (a > 0) c.fill(x + cx, y + r, x + e, y + r + 1, ThemeManager.argb(rgb, alpha * a / 15f));
                cx = e;
            }
        }
    }
}
