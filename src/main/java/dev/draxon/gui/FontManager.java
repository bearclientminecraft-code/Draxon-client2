package dev.draxon.gui;
import dev.draxon.core.ConfigManager;
import dev.draxon.core.DraxonClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.nio.file.*;
import java.util.*;
/**
 * Bundled fonts: "Sans" and "Sans Bold" (Instrument Sans, OFL) via font JSON in assets/draxon/font.
 * Custom fonts: list font ids in config/draxon/fonts.txt, one per line, "Display Name=namespace:fontid"
 * (or just "namespace:fontid"). The font itself must be defined by a resource pack (ttf provider).
 */
public final class FontManager {
    public static final String VANILLA = "Minecraft";
    private static final List<String> names = new ArrayList<>(List.of("Sans", "Sans Bold", VANILLA));
    private static final Map<String, Identifier> ids = new HashMap<>();
    private static final Map<String, Text> cache = new HashMap<>();
    private static String cachedFor = "";
    private FontManager() {}
    public static void init() {
        ids.put("Sans", Identifier.of("draxon", "sans")); ids.put("Sans Bold", Identifier.of("draxon", "sans_bold"));
        try {
            Path p = ConfigManager.DIR.resolve("fonts.txt");
            if (Files.exists(p)) for (String line : Files.readAllLines(p)) {
                line = line.trim(); if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('='); String disp = eq > 0 ? line.substring(0, eq).trim() : line;
                Identifier id = Identifier.tryParse(eq > 0 ? line.substring(eq + 1).trim() : line);
                if (id != null && !ids.containsKey(disp) && !names.contains(disp)) { ids.put(disp, id); names.add(disp); }
            }
        } catch (Exception ex) { dev.draxon.core.Draxon.LOGGER.error("Failed to read fonts.txt", ex); }
    }
    public static String[] names() { return names.toArray(new String[0]); }
    private static String mode() { return DraxonClient.theme.font.get(); }
    private static Text styled(String mode, String s) {
        if (!mode.equals(cachedFor) || cache.size() > 2048) { cache.clear(); cachedFor = mode; }
        Text t = cache.get(s);
        if (t == null) {
            // MOST UNVERIFIED LINE: Style.withFont signature changed in 1.21.9+ (StyleSpriteSource.Font).
            t = Text.literal(s).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(ids.get(mode))));
            cache.put(s, t);
        }
        return t;
    }
    public static void draw(DrawContext c, String s, int x, int y, int color) {
        MinecraftClient mc = MinecraftClient.getInstance(); String m = mode();
        if (VANILLA.equals(m) || !ids.containsKey(m)) c.drawText(mc.textRenderer, s, x, y, color, false);
        else c.drawText(mc.textRenderer, styled(m, s), x, y, color, false);
    }
    public static int width(String s) {
        MinecraftClient mc = MinecraftClient.getInstance(); String m = mode();
        return (VANILLA.equals(m) || !ids.containsKey(m)) ? mc.textRenderer.getWidth(s) : mc.textRenderer.getWidth(styled(m, s));
    }

    /** custom=false forces the vanilla font (used by the NameTags "Custom Font" option). */
    public static void draw(DrawContext c, String s, int x, int y, int color, boolean custom) {
        if (custom) draw(c, s, x, y, color); else c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, color, false);
    }
    public static int width(String s, boolean custom) { return custom ? width(s) : MinecraftClient.getInstance().textRenderer.getWidth(s); }
    public static void drawOutlined(DrawContext c, String s, int x, int y, int color, boolean custom) {
        int o = ThemeManager.argb(0x000000, 0.85f);
        draw(c, s, x - 1, y, o, custom); draw(c, s, x + 1, y, o, custom); draw(c, s, x, y - 1, o, custom); draw(c, s, x, y + 1, o, custom);
        draw(c, s, x, y, color, custom);
    }
}
