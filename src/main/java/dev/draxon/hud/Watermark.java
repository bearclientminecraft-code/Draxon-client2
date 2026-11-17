package dev.draxon.hud;
import dev.draxon.core.Draxon;
import dev.draxon.gui.AnimationManager.Anim;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.Glyphs;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.misc.HudModule;
import dev.draxon.util.RenderUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.network.PlayerListEntry;
import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
/** Two-row pill watermark, centered at the top. Moves below the vanilla boss bars when they are present. */
final class Watermark {
    private static final Anim yAnim = new Anim(4);
    private static final String[][] texts = new String[2][8]; private static final int[][] icons = new int[2][8], widths = new int[2][8];
    private static final int[] counts = new int[2], rowW = new int[2];
    private static long lastRefresh; private static double prevX, prevZ; private static boolean prevInit; private static String version = "";
    private static Field bossField; private static boolean bossTried;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Watermark() {}

    /** Boss bar count, found by field TYPE (a Map) so it does not depend on mapping names. 0 if it cannot be read. */
    static int bossBarCount(MinecraftClient mc) {
        try {
            BossBarHud hud = mc.inGameHud.getBossBarHud();
            if (!bossTried) { bossTried = true; for (Field f : BossBarHud.class.getDeclaredFields()) if (Map.class.isAssignableFrom(f.getType())) { f.setAccessible(true); bossField = f; break; } }
            if (bossField != null) return ((Map<?, ?>) bossField.get(hud)).size();
        } catch (ReflectiveOperationException | RuntimeException ex) { bossField = null; }
        return 0;
    }
    /** Mirrors vanilla: first bar at y=12, 19px per bar, stops once y >= height/3; each bar is 5px tall. */
    static int bossBarBottom(int count, int screenH) {
        int j = 12, bottom = 0;
        for (int i = 0; i < count; i++) { bottom = j + 5; j += 19; if (j >= screenH / 3) break; }
        return bottom;
    }
    private static void add(int r, int icon, String s) { int i = counts[r]++; texts[r][i] = s; icons[r][i] = icon; widths[r][i] = FontManager.width(s) + (icon >= 0 ? 10 : 0); }
    private static void refresh(MinecraftClient mc, HudModule h) {
        long now = System.currentTimeMillis(); if (now - lastRefresh < 250) return;
        double secs = Math.max(0.001, (now - lastRefresh) / 1000.0); lastRefresh = now;
        double bps = prevInit ? Math.hypot(mc.player.getX() - prevX, mc.player.getZ() - prevZ) / secs : 0; prevInit = true; prevX = mc.player.getX(); prevZ = mc.player.getZ();
        if (version.isEmpty()) version = FabricLoader.getInstance().getModContainer(Draxon.ID).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        counts[0] = counts[1] = 0;
        String nm = (h.showName.get() ? "Draxon Reborn" : "") + (h.showVersion.get() ? (h.showName.get() ? " v" : "v") + version : "");
        if (!nm.isEmpty()) add(0, -1, nm);
        if (h.showPlayer.get()) add(0, Glyphs.USER, mc.player.getName().getString());
        if (h.showFps.get()) add(0, Glyphs.FPS, mc.getCurrentFps() + " FPS");
        if (h.showPing.get()) { PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()); add(0, Glyphs.PING, (e == null ? 0 : e.getLatency()) + " ms"); }
        if (h.showServer.get()) add(0, Glyphs.SERVER, mc.getCurrentServerEntry() == null ? "Singleplayer" : mc.getCurrentServerEntry().address);
        if (h.showTime.get()) add(0, Glyphs.CLOCK, LocalTime.now().format(TIME));
        if (h.showCoords.get()) add(1, Glyphs.PIN, "x " + mc.player.getBlockX() + " y " + mc.player.getBlockY() + " z " + mc.player.getBlockZ());
        if (h.showBps.get()) add(1, Glyphs.SPEED, String.format(Locale.ROOT, "%.1f BPS", bps));
        for (int r = 0; r < 2; r++) { int w = 0; for (int i = 0; i < counts[r]; i++) w += widths[r][i] + (i > 0 ? 13 : 0); rowW[r] = w; }
    }

    static void render(DrawContext c, MinecraftClient mc, HudModule h, ThemeManager t, float dt, int sw, int sh) {
        refresh(mc, h);
        float s = h.scale.get().floatValue(); int pad = 4, rowH = 9 + pad * 2;
        float target = 4;
        if (h.bossAdapt.get()) { int n = bossBarCount(mc); if (n > 0) target = bossBarBottom(n, sh) + 4; } // always overlaps horizontally: both are centered
        yAnim.target = target; yAnim.update(dt, 10f);
        c.getMatrices().pushMatrix(); c.getMatrices().translate(sw / 2f, yAnim.value); c.getMatrices().scale(s, s);
        int y = 0;
        for (int r = 0; r < 2; r++) {
            if (counts[r] == 0) continue;
            int w = rowW[r] + pad * 2, x0 = -w / 2;
            if (h.background.get()) {
                if (h.shadow.get()) RenderUtils.shadow(c, x0, y, w, rowH, rowH / 2);
                RenderUtils.roundRect(c, x0, y, w, rowH, rowH / 2, ThemeManager.argb(t.panel, t.op()));
                RenderUtils.frost(c, x0, y, w, rowH, rowH / 2, t.frost.get().floatValue(), 1f);
                if (h.border.get()) RenderUtils.border(c, x0, y, w, rowH, rowH / 2, t.border != 0 ? ThemeManager.argb(t.border, 0.5f) : ThemeManager.argb(0xFFFFFF, 0.10f));
            }
            int x = x0 + pad;
            for (int i = 0; i < counts[r]; i++) {
                if (i > 0) c.fill(x - 7, y + 4, x - 6, y + rowH - 4, ThemeManager.argb(0xFFFFFF, 0.12f));
                int ic = icons[r][i];
                if (ic >= 0 && h.icons.get()) Glyphs.draw(c, ic, x, y + (rowH - 7) / 2, t.flower, 1f);
                FontManager.draw(c, texts[r][i], x + (ic >= 0 ? 10 : 0), y + (rowH - 9) / 2, ThemeManager.argb(ic < 0 ? t.acc() : t.text, 1f));
                x += widths[r][i] + 13;
            }
            y += rowH + 3;
        }
        c.getMatrices().popMatrix();
    }
}
