package dev.draxon.setting;
import java.awt.Color;
/** RGB color with cached HSB so hue/saturation survive when brightness or saturation hits 0. */
public class ColorSetting extends Setting<Integer> {
    public float h, s, b;
    public ColorSetting(String n, int rgb) { super(n, rgb & 0xFFFFFF); sync(); }
    private void sync() {
        float[] f = Color.RGBtoHSB(value >> 16 & 255, value >> 8 & 255, value & 255, null);
        h = f[0]; s = f[1]; b = f[2];
    }
    @Override public void set(Integer v) { value = v & 0xFFFFFF; sync(); }
    public void setHsb(float h, float s, float b) { this.h = h; this.s = s; this.b = b; value = Color.HSBtoRGB(h, s, b) & 0xFFFFFF; }
    @Override public String serialize() { return String.format("%06X", value); }
    @Override public void deserialize(String str) { set(Integer.parseInt(str, 16)); }
}
