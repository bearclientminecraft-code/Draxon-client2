package dev.draxon.render;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
/** Real item icons via DrawContext.drawItem; count/durability via vanilla's own overlay so layering is correct. */
public final class ItemRenderUtils {
    private ItemRenderUtils() {}
    public static void drawStack(DrawContext c, ItemStack s, int x, int y, boolean count, boolean durability) {
        if (s.isEmpty()) return;
        c.drawItem(s, x, y);
        boolean wantCount = count && s.getCount() > 1, wantDur = durability && s.isDamageable() && s.getDamage() > 0;
        if (wantCount || wantDur) c.drawStackOverlay(MinecraftClient.getInstance().textRenderer, s, x, y);
    }
    /** Up to 3 short enchant labels (e.g. "pr4") stacked above an item; called under a scaled matrix. */
    public static int drawEnchants(DrawContext c, ItemStack s, int cx, int bottomY, boolean custom) {
        if (s.isEmpty() || !s.hasEnchantments()) return 0;
        ItemEnchantmentsComponent ench = s.getEnchantments(); int line = 0;
        for (RegistryEntry<?> e : ench.getEnchantments()) {
            if (line >= 3) break;
            @SuppressWarnings("unchecked") int lvl = ench.getLevel((RegistryEntry) e);
            String id = e.getIdAsString(); id = id.substring(id.indexOf(':') + 1);
            String t = id.substring(0, Math.min(2, id.length())) + lvl;
            line++;
            FontManager.draw(c, t, cx - FontManager.width(t, custom) / 2, bottomY - line * 8, ThemeManager.argb(0xFFD866, 1f), custom);
        }
        return line * 8;
    }
}
