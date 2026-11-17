package dev.draxon.gui;
import dev.draxon.setting.*;
import java.util.List;
public final class ThemeManager {
    public final NumberSetting opacity = new NumberSetting("Panel opacity", 0.82, 0.2, 1.0, 0.01);
    public final NumberSetting frost = new NumberSetting("Frost", 0.6, 0.0, 1.0, 0.05);
    public final NumberSetting radius = new NumberSetting("Corner radius", 6, 0, 12, 1);
    public final NumberSetting scale = new NumberSetting("GUI scale", 1.0, 0.6, 1.6, 0.05);
    public final ColorSetting accent = new ColorSetting("Accent", 0x79B9F5);
    public final ModeSetting font = new ModeSetting("Font", "Sans", FontManager.names());
    public final List<Setting<?>> settings = List.of(accent, opacity, frost, radius, scale, font);
    // fixed palette (Dark Glass)
    public final int background = 0x090D18, panel = 0x101824, secondary = 0x151F2D, text = 0xE8EDF5, textDim = 0x929EAE, disabled = 0x687587, track = 0x1B2636;
    public int acc() { return accent.get(); }
    public float op() { return opacity.get().floatValue(); }
    public int rad() { return radius.get().intValue(); }
    public static int argb(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }
}
