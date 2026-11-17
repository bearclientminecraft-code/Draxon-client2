package dev.draxon.gui;
import dev.draxon.setting.*;
import java.util.List;
import java.util.function.BooleanSupplier;
public final class ThemeManager {
    public final NumberSetting opacity = new NumberSetting("Panel opacity", 0.82, 0.2, 1.0, 0.01);
    public final NumberSetting frost = new NumberSetting("Frost", 0.6, 0.0, 1.0, 0.05);
    public final NumberSetting radius = new NumberSetting("Corner radius", 6, 0, 12, 1);
    public final NumberSetting scale = new NumberSetting("GUI scale", 1.0, 0.6, 1.6, 0.05);
    public final ModeSetting font = new ModeSetting("Font", "Sans", FontManager.names());
    /** Menu background blur is expensive on weak GPUs (phones): off by default. */
    public final BooleanSetting blur = new BooleanSetting("Background blur", false);
    /** Hard-edged small shapes (switches, sliders, hover rows). Panel corners stay anti-aliased. */
    public final BooleanSetting fastRender = new BooleanSetting("Fast rendering", true);
    public final List<Setting<?>> settings = List.of(opacity, frost, radius, scale, font, blur, fastRender);
    /** Live palette, recomputed by sync() when the Themes module state changes. */
    public int background, panel, secondary, track, accentRgb, flower, border;
    public final int text = 0xE8EDF5, textDim = 0x929EAE, disabled = 0x687587;
    private ThemeSelectSetting sel; private ColorSetting cAccent, cPanel; private BooleanSupplier enabled;
    private String lastName = ""; private int lastA = -1, lastP = -1; private boolean lastOn = true;
    public ThemeManager() { derive(0x101824, 0x79B9F5, 0x79B9F5, 0); }
    public void bind(ThemeSelectSetting s, ColorSetting accent, ColorSetting panelColor, BooleanSupplier on) { sel = s; cAccent = accent; cPanel = panelColor; enabled = on; lastName = ""; sync(); }
    public int acc() { return accentRgb; }
    public float op() { return opacity.get().floatValue(); }
    public int rad() { return radius.get().intValue(); }
    public static int argb(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }
    public static int mix(int a, int b, float k) {
        int r = (int) ((a >> 16 & 255) * (1 - k) + (b >> 16 & 255) * k), g = (int) ((a >> 8 & 255) * (1 - k) + (b >> 8 & 255) * k), bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return r << 16 | g << 8 | bl;
    }
    private void derive(int bg, int accent, int flowerCol, int borderCol) {
        panel = bg; background = mix(bg, 0x000000, 0.45f); secondary = mix(bg, 0xFFFFFF, 0.07f); track = mix(bg, 0xFFFFFF, 0.13f);
        accentRgb = accent; flower = flowerCol; border = borderCol;
    }
    /** Cheap; call once per frame. Themes module disabled -> original Dark Glass palette. */
    public void sync() {
        dev.draxon.util.RenderUtils.fast = fastRender.get();
        if (sel == null) return;
        boolean on = enabled.getAsBoolean(); String n = sel.get(); int a = cAccent.get(), p = cPanel.get();
        if (on == lastOn && n.equals(lastName) && a == lastA && p == lastP) return;
        lastOn = on; lastName = n; lastA = a; lastP = p;
        if (!on) { derive(0x101824, 0x79B9F5, 0x79B9F5, 0); return; }
        Theme t = Theme.find(n);
        if (t == null) derive(p, a, a, 0); else derive(t.bg(), t.accent(), t.flower(), t.border());
    }
}
