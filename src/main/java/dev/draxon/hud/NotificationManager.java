package dev.draxon.hud;
import dev.draxon.core.DraxonClient;
import dev.draxon.core.EventBus;
import dev.draxon.core.Events;
import dev.draxon.gui.AnimationManager.Anim;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.Glyphs;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.misc.HudModule;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;
/** Glass notifications stacked downwards under the crosshair (screen center + offset). */
public final class NotificationManager {
    public static final int SUCCESS = 0, WARNING = 1; private static final int NH = 26, WARN_COLOR = 0xF2B45A;
    private static final class N { String title, desc; int type; float age; boolean leaving, placed; int w; final Anim alpha = new Anim(0), pos = new Anim(0); }
    private static final List<N> list = new ArrayList<>();
    private NotificationManager() {}
    static void init() {
        EventBus.INSTANCE.subscribe(Events.ModuleToggle.class, e -> {
            HudModule h = DraxonClient.modules == null ? null : DraxonClient.modules.get(HudModule.class);
            if (h == null || e.module() == h || MinecraftClient.getInstance().world == null || !h.isEnabled() || !h.notifications.get()) return;
            push(e.module().name, e.enabled() ? "Module enabled" : "Module disabled", e.enabled() ? SUCCESS : WARNING);
        });
    }
    public static void push(String title, String desc, int type) {
        N n = new N(); n.title = title; n.desc = desc; n.type = type; n.alpha.target = 1; list.add(n);
        HudModule h = DraxonClient.modules.get(HudModule.class); int max = h == null ? 4 : h.notifMax.get().intValue(), live = 0;
        for (int i = list.size() - 1; i >= 0; i--) if (!list.get(i).leaving && ++live > max) list.get(i).leaving = true;
    }
    static void render(DrawContext c, HudModule h, ThemeManager t, float dt, int sw, int sh) {
        float s = h.scale.get().floatValue(), dur = h.notifDuration.get().floatValue(); int sp = h.notifSpacing.get().intValue(), off = h.notifOffset.get().intValue(), idx = 0;
        for (int i = list.size() - 1; i >= 0; i--) {
            N n = list.get(i); n.age += dt; if (n.age > dur) n.leaving = true;
            n.alpha.target = n.leaving ? 0 : 1; n.alpha.update(dt, 10f);
            float tp = idx * (NH * s + sp); if (!n.placed) { n.pos.value = tp; n.placed = true; } n.pos.target = tp; n.pos.update(dt, 12f);
            if (!n.leaving) idx++;
            float a = Math.max(0f, Math.min(1f, n.alpha.value)); if (a < 0.01f) continue;
            int accent = n.type == SUCCESS ? t.acc() : WARN_COLOR;
            if (n.w == 0) n.w = Math.max(104, 34 + Math.max(FontManager.width(n.title), FontManager.width(n.desc)) + 8);
            int w = n.w;
            float x = sw / 2f - w * s / 2f, y = sh / 2f + off + n.pos.value + (1f - a) * 8f;
            c.getMatrices().pushMatrix(); c.getMatrices().translate(x, y); c.getMatrices().scale(s, s);
            RenderUtils.glassPanel(c, 0, 0, w, NH, t, a);
            RenderUtils.roundRect(c, 6, 6, 14, 14, 7, ThemeManager.argb(accent, 0.22f * a));
            Glyphs.draw(c, n.type == SUCCESS ? Glyphs.CHECK : Glyphs.WARN, 9, 9, accent, a);
            FontManager.draw(c, n.title, 26, 4, ThemeManager.argb(t.text, a));
            FontManager.draw(c, n.desc, 26, 14, ThemeManager.argb(t.textDim, a));
            c.fill(6, NH - 3, 6 + (int) ((w - 12) * Math.max(0f, 1f - n.age / dur)), NH - 2, ThemeManager.argb(accent, 0.7f * a));
            c.getMatrices().popMatrix();
        }
        for (int i = list.size() - 1; i >= 0; i--) { N n = list.get(i); if (n.leaving && n.alpha.value < 0.01f) list.remove(i); }
    }
}
