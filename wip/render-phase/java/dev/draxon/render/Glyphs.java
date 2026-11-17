package dev.draxon.render;
import dev.draxon.gui.ThemeManager;
import net.minecraft.client.gui.DrawContext;
/** 7x7 pixel glyphs for HUD/notification/nametag icons (tintable, no textures). */
public final class Glyphs {
    public static final int USER = 0, FPS = 1, PING = 2, CLOCK = 3, PIN = 4, SPEED = 5, SERVER = 6, HEART = 7, CHECK = 8, WARN = 9, STAR = 10;
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
        {"0001000","0001000","1111111","0011100","0111110","0100010","0000000"}};
    private Glyphs() {}
    public static void draw(DrawContext c, int id, int x, int y, int rgb, float alpha) {
        int col = ThemeManager.argb(rgb, alpha);
        for (int r = 0; r < 7; r++) { String row = G[id][r]; int s = -1;
            for (int i = 0; i <= 7; i++) {
                boolean on = i < 7 && row.charAt(i) == '1';
                if (on && s < 0) s = i; else if (!on && s >= 0) { c.fill(x + s, y + r, x + i, y + r + 1, col); s = -1; }
            } }
    }
}
