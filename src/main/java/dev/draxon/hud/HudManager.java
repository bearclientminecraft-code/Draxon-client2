package dev.draxon.hud;
import dev.draxon.core.ConfigManager;
import dev.draxon.core.DraxonClient;
import dev.draxon.gui.Glyphs;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.Module;
import dev.draxon.module.misc.HudModule;
import dev.draxon.setting.KeybindSetting;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.glfw.GLFW;
/** One HUD callback drives watermark, potion list, keybinds and notifications. While chat is open the two lists can be dragged. */
public final class HudManager {
    private static long last = System.nanoTime(), lastRows;
    private static final ListPanel potions = new ListPanel("Potion-list", Glyphs.POTION), keys = new ListPanel("KeyBinds", Glyphs.KEY);
    private static ListPanel dragging; private static int dragDx, dragDy; private static boolean wasDown;
    private HudManager() {}
    public static void init() { NotificationManager.init(); HudRenderCallback.EVENT.register(HudManager::onHud); }

    private static void onHud(DrawContext c, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime(); float dt = Math.min((now - last) / 1e9f, 0.1f); last = now;
        if (mc.player == null || mc.world == null || mc.options.hudHidden || DraxonClient.modules == null) return;
        if (mc.currentScreen instanceof dev.draxon.gui.ClickGuiScreen) return; // the menu is a full-screen overlay: do not pay for the HUD behind it
        ThemeManager t = DraxonClient.theme; t.sync();
        HudModule h = DraxonClient.modules.get(HudModule.class); if (h == null || !h.isEnabled()) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight(); float s = h.scale.get().floatValue();
        boolean chat = mc.currentScreen instanceof ChatScreen;
        potions.xs = h.potionX; potions.ys = h.potionY; keys.xs = h.keysX; keys.ys = h.keysY;
        long ms = System.currentTimeMillis(); if (ms - lastRows > 200) { lastRows = ms; refreshRows(mc, h, chat); }
        if (h.watermark.get()) Watermark.render(c, mc, h, t, dt, sw, sh);
        potions.draw(c, t, s, sw, sh, chat, dt, h.potionList.get());
        keys.draw(c, t, s, sw, sh, chat, dt, h.keybinds.get());
        if (chat) drag(mc, sw, sh); else dragging = null;
        if (h.notifications.get()) NotificationManager.render(c, h, t, dt, sw, sh);
    }

    private static void refreshRows(MinecraftClient mc, HudModule h, boolean chat) {
        potions.clear(); keys.clear();
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            int secs = Math.max(0, e.getDuration() / 20);
            potions.add(e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1), e.isInfinite() ? "--" : (secs / 60) + ":" + (secs % 60 < 10 ? "0" : "") + (secs % 60), e.getEffectType().value().getColor() | 0xFF000000, false);
        }
        if (potions.pending() == 0 && chat) { potions.add("Speed 2", "0:30", 0x7CAFC6, false); potions.add("Strength 1", "1:12", 0x932423, false); } // preview so it can be moved
        for (Module m : DraxonClient.modules.all()) {
            int k = m.bind.get(); if (k < 0 || (!m.isEnabled() && !h.keybindsAll.get())) continue;
            keys.add(m.name, "[" + KeybindSetting.keyName(k) + "]", m.isEnabled() ? DraxonClient.theme.acc() : DraxonClient.theme.disabled, !m.isEnabled());
        }
        if (keys.pending() == 0 && chat) keys.add("Example", "[R]", DraxonClient.theme.acc(), false);
        potions.commit(); keys.commit();
    }

    /** Mouse is polled (no mixin into ChatScreen): left button drags a panel by its body. Positions saved as screen ratios. */
    private static void drag(MinecraftClient mc, int sw, int sh) {
        double k = (double) sw / mc.getWindow().getWidth();
        int mx = (int) (mc.mouse.getX() * k), my = (int) (mc.mouse.getY() * k);
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            if (keys.contains(mx, my)) dragging = keys; else if (potions.contains(mx, my)) dragging = potions; else dragging = null;
            if (dragging != null) { dragDx = mx - dragging.rx; dragDy = my - dragging.ry; }
        }
        if (down && dragging != null) {
            dragging.xs.set((double) Math.max(0, Math.min(sw - dragging.rw, mx - dragDx)) / sw);
            dragging.ys.set((double) Math.max(0, Math.min(sh - dragging.rh, my - dragDy)) / sh);
        }
        if (!down && dragging != null) { dragging = null; ConfigManager.markDirty(); }
        wasDown = down;
    }
}
