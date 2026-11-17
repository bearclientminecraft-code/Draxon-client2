package dev.draxon.gui;
import dev.draxon.gui.AnimationManager.Anim;
import dev.draxon.setting.*;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
/** Draws and handles every setting component (shared by module settings and the Interface panel). Coordinates are GUI-space. */
final class SettingRenderer {
    private final ThemeManager t;
    float dt;
    Setting<?> listening, typing, dragging; int dragPart, dragX, dragW;
    private final Map<Object, Anim> anims = new HashMap<>();
    SettingRenderer(ThemeManager t) { this.t = t; }

    static int height(Setting<?> s) {
        if (s instanceof NumberSetting) return 17;
        if (s instanceof StringSetting) return 26;
        if (s instanceof ColorSetting) return 14 + 3 * 9;
        if (s instanceof MultiSetting m) return 14 + m.options.length * 12;
        if (s instanceof LabelSetting) return 13;
        if (s instanceof ThemeSelectSetting) return Theme.ALL.length * 17 + 19;
        return 14;
    }
    boolean capturing() { return listening != null || typing != null; }
    void clearFocus() { listening = null; typing = null; }
    private static boolean hit(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private float anim(Object key, float target) {
        Anim a = anims.computeIfAbsent(key, k -> new Anim(target)); a.target = target; a.update(dt, 20f); return a.value;
    }
    private static String fmt(double v) { return Math.abs(v - Math.rint(v)) < 1e-9 ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%.2f", v); }

    void draw(DrawContext c, Setting<?> s, int x, int y, int w, float a) {
        int dim = ThemeManager.argb(t.textDim, a), txt = ThemeManager.argb(t.text, a), acc = t.acc();
        if (s instanceof BooleanSetting b) {
            float k = anim(b, b.get() ? 1 : 0);
            FontManager.draw(c, s.name, x, y + 3, dim);
            RenderUtils.roundRect(c, x + w - 16, y + 3, 16, 8, 4, ThemeManager.argb(mix(t.track, acc, k), a));
            RenderUtils.roundRect(c, x + w - 15 + (int) (8 * k), y + 4, 6, 6, 3, ThemeManager.argb(0xFFFFFF, a));
        } else if (s instanceof NumberSetting n) {
            String v = fmt(n.get()) + n.unit;
            FontManager.draw(c, s.name, x, y + 1, dim);
            FontManager.draw(c, v, x + w - FontManager.width(v), y + 1, txt);
            int fill = Math.max(2, (int) (w * n.progress()));
            RenderUtils.roundRect(c, x, y + 12, w, 3, 1, ThemeManager.argb(t.track, a));
            RenderUtils.roundRect(c, x, y + 12, fill, 3, 1, ThemeManager.argb(acc, a));
            RenderUtils.roundRect(c, x + fill - 3, y + 11, 5, 5, 2, ThemeManager.argb(0xFFFFFF, a));
        } else if (s instanceof ModeSetting m) {
            FontManager.draw(c, s.name, x, y + 3, dim);
            FontManager.draw(c, m.get(), x + w - FontManager.width(m.get()), y + 3, ThemeManager.argb(acc, a));
        } else if (s instanceof KeybindSetting k) {
            String v = listening == s ? "..." : KeybindSetting.keyName(k.get()); int bw = FontManager.width(v) + 8;
            FontManager.draw(c, s.name, x, y + 3, dim);
            RenderUtils.roundRect(c, x + w - bw, y + 1, bw, 12, 3, ThemeManager.argb(t.secondary, a));
            if (listening == s) RenderUtils.border(c, x + w - bw, y + 1, bw, 12, 3, ThemeManager.argb(acc, a));
            FontManager.draw(c, v, x + w - bw + 4, y + 3, txt);
        } else if (s instanceof StringSetting st) {
            FontManager.draw(c, s.name, x, y + 1, dim);
            RenderUtils.roundRect(c, x, y + 11, w, 13, 3, ThemeManager.argb(t.secondary, a));
            if (typing == s) RenderUtils.border(c, x, y + 11, w, 13, 3, ThemeManager.argb(acc, a));
            String v = st.get(); boolean cursor = typing == s && System.currentTimeMillis() / 500 % 2 == 0;
            while (v.length() > 1 && FontManager.width(v) > w - 12) v = v.substring(1);
            if (v.isEmpty() && typing != s) FontManager.draw(c, "...", x + 4, y + 14, ThemeManager.argb(t.disabled, a));
            else FontManager.draw(c, cursor ? v + "_" : v, x + 4, y + 14, txt);
        } else if (s instanceof ColorSetting cs) {
            FontManager.draw(c, s.name, x, y + 3, dim);
            RenderUtils.roundRect(c, x + w - 16, y + 2, 16, 10, 3, ThemeManager.argb(cs.get(), a));
            RenderUtils.border(c, x + w - 16, y + 2, 16, 10, 3, ThemeManager.argb(0xFFFFFF, 0.2f * a));
            for (int i = 0; i < 3; i++) bar(c, x, y + 14 + i * 9, w, i, cs, a);
        } else if (s instanceof MultiSetting m) {
            FontManager.draw(c, s.name, x, y + 3, dim);
            for (int i = 0; i < m.options.length; i++) {
                int oy = y + 14 + i * 12; String o = m.options[i];
                float k = anim(o + "@" + System.identityHashCode(m), m.has(o) ? 1 : 0);
                RenderUtils.roundRect(c, x, oy + 2, 8, 8, 2, ThemeManager.argb(mix(t.secondary, acc, k), a));
                FontManager.draw(c, o, x + 12, oy + 2, m.has(o) ? txt : dim);
            }
        } else if (s instanceof LabelSetting) {
            FontManager.draw(c, s.name, x, y + 3, ThemeManager.argb(acc, a));
            c.fill(x + FontManager.width(s.name) + 4, y + 7, x + w, y + 8, ThemeManager.argb(acc, 0.25f * a));
        } else if (s instanceof ThemeSelectSetting ts) {
            for (int i = 0; i < Theme.ALL.length; i++) {
                Theme th = Theme.ALL[i]; int ry = y + i * 17; boolean on = th.name().equals(ts.get());
                RenderUtils.roundRect(c, x, ry, w, 15, 4, ThemeManager.argb(th.bg(), 0.95f * a));
                RenderUtils.border(c, x, ry, w, 15, 4, ThemeManager.argb(on ? th.accent() : 0xFFFFFF, (on ? 0.95f : 0.10f) * a));
                RenderUtils.roundRect(c, x + 5, ry + 4, 7, 7, 3, ThemeManager.argb(th.accent(), a));
                Glyphs.draw(c, Glyphs.FLOWER, x + 16, ry + 4, th.flower(), a);
                FontManager.draw(c, th.name(), x + 28, ry + 3, txt);
                if (on) Glyphs.draw(c, Glyphs.CHECK, x + w - 12, ry + 4, th.accent(), a);
            }
            int ry = y + Theme.ALL.length * 17 + 2; boolean on = ts.isCustom();
            RenderUtils.roundRect(c, x, ry, w, 15, 4, ThemeManager.argb(on ? t.panel : t.secondary, 0.95f * a));
            RenderUtils.border(c, x, ry, w, 15, 4, ThemeManager.argb(on ? t.acc() : 0xFFFFFF, (on ? 0.95f : 0.10f) * a));
            RenderUtils.roundRect(c, x + 4, ry + 4, 7, 7, 3, ThemeManager.argb(on ? t.acc() : 0x8B5CF6, a));
            RenderUtils.roundRect(c, x + 9, ry + 4, 7, 7, 3, ThemeManager.argb(on ? t.panel : 0x14141C, a));
            RenderUtils.border(c, x + 9, ry + 4, 7, 7, 3, ThemeManager.argb(0xFFFFFF, 0.25f * a));
            FontManager.draw(c, "Custom", x + 21, ry + 3, txt);
            c.getMatrices().pushMatrix(); c.getMatrices().translate(x + w - 5, ry + 5); c.getMatrices().scale(0.75f, 0.75f);
            FontManager.draw(c, "Edit below", -FontManager.width("Edit below"), 0, dim);
            c.getMatrices().popMatrix();
        }
    }
    private void bar(DrawContext c, int x, int y, int w, int mode, ColorSetting cs, float a) {
        for (int px = 0; px < w; px += 2) {
            float p = px / (float) w;
            int rgb = mode == 0 ? Color.HSBtoRGB(p, 1f, 1f) : mode == 1 ? Color.HSBtoRGB(cs.h, p, cs.b) : Color.HSBtoRGB(cs.h, cs.s, p);
            c.fill(x + px, y, x + Math.min(px + 2, w), y + 6, ThemeManager.argb(rgb, a));
        }
        float pos = mode == 0 ? cs.h : mode == 1 ? cs.s : cs.b;
        int hx = x + Math.min(w - 2, Math.max(1, (int) (pos * w)));
        c.fill(hx - 1, y - 1, hx + 1, y + 7, ThemeManager.argb(0xFFFFFF, a));
    }
    static int mix(int a, int b, float k) {
        int r = (int) ((a >> 16 & 255) * (1 - k) + (b >> 16 & 255) * k), g = (int) ((a >> 8 & 255) * (1 - k) + (b >> 8 & 255) * k), bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return r << 16 | g << 8 | bl;
    }

    boolean click(Setting<?> s, int x, int y, int w, int mx, int my, int button) {
        if (!hit(mx, my, x, y, w, height(s))) return false;
        if (s instanceof BooleanSetting b) { if (button == 0) b.toggle(); }
        else if (s instanceof NumberSetting) { if (button == 0) { dragging = s; dragPart = 0; dragX = x; dragW = w; dragTo(mx); } }
        else if (s instanceof ModeSetting m) { m.cycle(button == 1 ? -1 : 1); }
        else if (s instanceof KeybindSetting k) { if (button == 1) k.set(-1); else listening = s; }
        else if (s instanceof StringSetting) { if (my >= y + 11) typing = s; }
        else if (s instanceof ColorSetting) {
            for (int i = 0; i < 3; i++) if (hit(mx, my, x, y + 13 + i * 9, w, 9)) { dragging = s; dragPart = i + 1; dragX = x; dragW = w; dragTo(mx); }
        } else if (s instanceof MultiSetting m) {
            for (int i = 0; i < m.options.length; i++) if (hit(mx, my, x, y + 14 + i * 12, w, 12)) m.toggle(m.options[i]);
        } else if (s instanceof ThemeSelectSetting ts) {
            if (button == 0) { int rel = my - y; if (rel >= Theme.ALL.length * 17) ts.set(ThemeSelectSetting.CUSTOM); else ts.set(Theme.ALL[Math.max(0, rel / 17)].name()); }
        }
        return true;
    }
    void dragTo(int mx) {
        if (dragging == null) return;
        double p = Math.max(0, Math.min(1, (mx - dragX) / (double) dragW));
        if (dragging instanceof NumberSetting n) n.setProgress(p);
        else if (dragging instanceof ColorSetting cs) {
            float f = (float) p;
            if (dragPart == 1) cs.setHsb(f, cs.s, cs.b); else if (dragPart == 2) cs.setHsb(cs.h, f, cs.b); else cs.setHsb(cs.h, cs.s, f);
        }
    }
    void release() { dragging = null; }
    /** Returns true if the key was consumed by a focused component. */
    boolean key(int key) {
        if (listening instanceof KeybindSetting k) {
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) k.set(-1);
            else if (key != GLFW.GLFW_KEY_ESCAPE) k.set(key);
            listening = null; return true;
        }
        if (typing instanceof StringSetting st) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !st.get().isEmpty()) st.set(st.get().substring(0, st.get().length() - 1));
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) typing = null;
            return true;
        }
        return false;
    }
    boolean typed(String str) {
        if (typing instanceof StringSetting st) { for (int i = 0; i < str.length(); i++) if (str.charAt(i) < 32) return true; st.set(st.get() + str); return true; }
        return false;
    }
}
