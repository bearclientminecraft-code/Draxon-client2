package dev.draxon.render;
import dev.draxon.core.DraxonClient;
import dev.draxon.core.EventBus;
import dev.draxon.core.Events;
import dev.draxon.gui.AnimationManager.Anim;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.render.HudModule;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;
public final class NotificationManager {
    public static final int SUCCESS = 0, WARNING = 1;
    private static final int W = 124, H = 28, MARGIN = 6;
    private static final class N { String title, desc; int type; float age; boolean leaving; final Anim slide = new Anim(1), pos = new Anim(0), alpha = new Anim(0); boolean placed; }
    private static final List<N> list = new ArrayList<>();
    private NotificationManager() {}
    public static void init() {
        EventBus.INSTANCE.subscribe(Events.ModuleToggle.class, e -> {
            HudModule h = hud(); MinecraftClient mc = MinecraftClient.getInstance();
            if (h == null || mc.world == null || !h.isEnabled() || !h.notifications.get() || !h.notifyToggle.get() || e.module() == h) return;
            push(e.module().name, e.enabled() ? "Module enabled" : "Module disabled", e.enabled() ? SUCCESS : WARNING);
        });
    }
    private static HudModule hud() { return DraxonClient.modules == null ? null : DraxonClient.modules.get(HudModule.class); }
    public static void push(String title, String desc, int type) {
        N n = new N(); n.title = title; n.desc = desc; n.type = type; n.slide.target = 0; n.alpha.target = 1; list.add(n);
        HudModule h = hud(); int max = h == null ? 5 : h.notifMax.get().intValue();
        for (int i = 0; i < list.size() && list.size() > max; i++) if (!list.get(i).leaving) list.get(i).leaving = true;
    }
    public static void render(DrawContext c, float dt, HudModule h, int sw, int sh) {
        ThemeManager t = DraxonClient.theme; float s = h.notifScale.get().floatValue(); int sp = h.notifSpacing.get().intValue();
        String pos = h.notifPos.get(); boolean right = pos.contains("Right"), bottom = pos.contains("Bottom"); float dur = h.notifDuration.get().floatValue();
        int idx = 0;
        for (int i = list.size() - 1; i >= 0; i--) {
            N n = list.get(i); n.age += dt;
            if (n.age > dur) n.leaving = true;
            if (n.leaving) { n.slide.target = 1; n.alpha.target = 0; }
            n.slide.update(dt, 10f); n.alpha.update(dt, 9f);
            float targetPos = idx * (H * s + sp); if (!n.placed) { n.pos.value = targetPos; n.placed = true; } n.pos.target = targetPos; n.pos.update(dt, 12f);
            if (!n.leaving) idx++;
            float a = Math.max(0f, Math.min(1f, n.alpha.value)); if (a < 0.01f) continue;
            float off = n.slide.value * (W * s + MARGIN);
            int x = right ? (int) (sw - MARGIN - W * s + off) : (int) (MARGIN - off);
            int y = bottom ? (int) (sh - MARGIN - H * s - n.pos.value) : (int) (MARGIN + n.pos.value);
            int accent = n.type == SUCCESS ? h.successColor.get() : h.warningColor.get();
            c.getMatrices().pushMatrix(); c.getMatrices().translate(x, y); c.getMatrices().scale(s, s);
            RenderUtils.glassPanel(c, 0, 0, W, H, t, a);
            RenderUtils.roundRect(c, 4, 6, 2, H - 12, 1, ThemeManager.argb(accent, a));
            Glyphs.draw(c, n.type == SUCCESS ? Glyphs.CHECK : Glyphs.WARN, 11, 10, accent, a);
            FontManager.draw(c, n.title, 24, 5, ThemeManager.argb(t.text, a));
            FontManager.draw(c, n.desc, 24, 15, ThemeManager.argb(t.textDim, a));
            float left = Math.max(0f, 1f - n.age / dur);
            c.fill(6, H - 3, 6 + (int) ((W - 12) * left), H - 2, ThemeManager.argb(accent, 0.7f * a));
            c.getMatrices().popMatrix();
        }
        for (int i = list.size() - 1; i >= 0; i--) { N n = list.get(i); if (n.leaving && n.alpha.value < 0.01f && n.slide.value > 0.98f) list.remove(i); }
    }
}
