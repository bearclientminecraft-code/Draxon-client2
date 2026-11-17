package dev.draxon.gui;
import net.minecraft.client.gui.DrawContext;
/** 7x7 pixel glyphs (tintable, no textures). */
public final class Glyphs {
    public static final int USER = 0, FPS = 1, PING = 2, CLOCK = 3, PIN = 4, SPEED = 5, SERVER = 6, HEART = 7, CHECK = 8, WARN = 9, FLOWER = 10, KEY = 11, POTION = 12, LIST = 13;
    private static final String[][] G = {
        {"0011100","0111110","0011100","0000000","0111110","1111111","1111111"},
        {"1111111","1000001","1000001","1000001","1111111","0011100","0111110"},
        {"0000001","0000001","0000101","0000101","0010101","0010101","1010101"},
        {"0011100","0100010","1001001","1001111","1000001","0100010","0011100"},
        {"0011100","0111110","0111110","0011100","0011100","0001000","0001000"},
        {"0001000","0011000","1111110","1111111","1111110","0011000","0001000"},
        {"1111111","1010101","1111111","0000000","1111111","1010101","1111111"},
        {"0110110","1111111","1111111","0111110","0011100","0001000","0000000"},
        {"0000001","0000011","1000110","1101100","0111000","0010000","0000000"},
        {"0001000","0011100","0011100","0110110","0110110","1111111","0000000"},
        {"0110110","1111111","0111110","1111111","0110110","0001000","0001000"},
        {"0111000","1000100","1000100","0111110","0001000","0001100","0001000"},
        {"0011100","0001000","0001000","0011100","0111110","1111111","0111110"},
        {"1011111","0000000","1011111","0000000","1011111","0000000","1011111"}};
    private static final int[][][] RECTS = new int[G.length][][];
    static {
        for (int id = 0; id < G.length; id++) {
            java.util.List<int[]> out = new java.util.ArrayList<>();
            for (int r = 0; r < 7; r++) {
                int s0 = -1;
                for (int i = 0; i <= 7; i++) {
                    boolean on = i < 7 && G[id][r].charAt(i) == '1';
                    if (on && s0 < 0) s0 = i;
                    else if (!on && s0 >= 0) {
                        boolean merged = false;
                        for (int[] q : out) if (q[3] == r && q[0] == s0 && q[2] == i) { q[3] = r + 1; merged = true; break; }
                        if (!merged) out.add(new int[]{s0, r, i, r + 1});
                        s0 = -1;
                    }
                }
            }
            RECTS[id] = out.toArray(new int[0][]);
        }
    }
    private Glyphs() {}
    public static void draw(DrawContext c, int id, int x, int y, int rgb, float alpha) {
        int col = ThemeManager.argb(rgb, alpha);
        for (int[] q : RECTS[id]) c.fill(x + q[0], y + q[1], x + q[2], y + q[3], col);
    }
}
