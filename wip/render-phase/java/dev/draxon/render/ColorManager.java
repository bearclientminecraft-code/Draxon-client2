package dev.draxon.render;
import java.awt.Color;
public final class ColorManager {
    private ColorManager() {}
    /** Smooth red -> yellow -> green. */
    public static int health(float ratio) { float r = Math.max(0f, Math.min(1f, ratio)); return Color.HSBtoRGB(r * 0.33f, 0.85f, 1f) & 0xFFFFFF; }
    public static int rainbow(float time, float offset, float sat) { return Color.HSBtoRGB(((time + offset) % 1f + 1f) % 1f, sat, 1f) & 0xFFFFFF; }
    public static int mix(int a, int b, float k) {
        int r = (int) ((a >> 16 & 255) * (1 - k) + (b >> 16 & 255) * k), g = (int) ((a >> 8 & 255) * (1 - k) + (b >> 8 & 255) * k), bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return r << 16 | g << 8 | bl;
    }
}
