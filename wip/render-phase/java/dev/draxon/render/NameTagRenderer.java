package dev.draxon.render;
import dev.draxon.core.DraxonClient;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.render.NameProtectModule;
import dev.draxon.module.render.NameTagsModule;
import dev.draxon.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
/**
 * Layout (local units, origin = bottom-center of the name row):
 *   [ helmet chest legs boots ]      <- armor row (+ enchant labels)
 *   [offhand] heart name [hp] [main] <- name row
 */
final class NameTagRenderer {
    private static final double[] out = new double[3];
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private NameTagRenderer() {}
    static void draw(DrawContext c, MinecraftClient mc, PlayerEntity p, float dist, float td, NameTagsModule m) {
        boolean cu = m.customFont.get();
        Vec3d pos = p.getLerpedPos(td);
        if (!ProjectionUtils.project(pos.x, pos.y + p.getHeight() + 0.35, pos.z, out)) return;
        float s = m.scale.get().floatValue() * (m.dynamicScale.get() ? (float) Math.max(0.55, Math.min(1.25, 8.0 / (dist + 4.0))) : 1f);
        float sx = (float) out[0], sy = (float) out[1] - m.vOffset.get().floatValue();
        if (sx < -200 || sx > ProjectionUtils.sw + 200 || sy < -100 || sy > ProjectionUtils.sh + 100) return;
        ThemeManager t = DraxonClient.theme;
        boolean friend = FriendManager.isFriend(p.getName().getString());
        float hp = p.getHealth() + p.getAbsorptionAmount(), ratio = Math.max(0f, Math.min(1f, p.getHealth() / Math.max(1f, p.getMaxHealth())));
        String name = NameProtectModule.forTags(p.getName().getString()); String hpS = "[" + (int) Math.ceil(hp) + "]";
        String dS = (int) dist + "m"; boolean heart = m.showHealth.get(), fi = friend && m.friendIndicator.get();
        int nameCol = ThemeManager.argb(friend ? m.friendColor.get() : 0xFFFFFF, 1f);
        int hpCol = ThemeManager.argb(m.healthColor.get() ? ColorManager.health(ratio) : 0xFFFFFF, 1f);
        int nW = m.showName.get() ? FontManager.width(name, cu) : 0, hW = m.showHealth.get() ? FontManager.width(hpS, cu) : 0, dW = m.showDistance.get() ? FontManager.width(dS, cu) : 0;
        int rowW = (fi ? 9 : 0) + (heart ? 10 : 0) + nW + (nW > 0 && hW > 0 ? 4 : 0) + hW + (dW > 0 ? 5 + dW : 0), rowH = 13;
        if (rowW == 0) rowW = 10;
        c.getMatrices().pushMatrix(); c.getMatrices().translate(sx, sy); c.getMatrices().scale(s, s);
        int x0 = -rowW / 2 - 4, w = rowW + 8;
        if (m.background.get()) {
            RenderUtils.shadow(c, x0, -rowH, w, rowH, 4);
            RenderUtils.roundRect(c, x0, -rowH, w, rowH, 4, ThemeManager.argb(t.panel, m.bgOpacity.get().floatValue()));
            RenderUtils.frost(c, x0, -rowH, w, rowH, 4, m.blur.get().floatValue(), 1f);
            RenderUtils.border(c, x0, -rowH, w, rowH, ThemeManager.argb(0xFFFFFF, 0.09f));
        }
        int x = -rowW / 2, ty = -rowH + 2;
        if (fi) { Glyphs.draw(c, Glyphs.STAR, x, ty + 1, m.friendColor.get(), 1f); x += 9; }
        if (heart) { Glyphs.draw(c, Glyphs.HEART, x, ty + 1, 0xFF4D5E, 1f); x += 10; }
        if (nW > 0) { text(c, m, name, x, ty, nameCol, cu); x += nW + 4; }
        if (hW > 0) { text(c, m, hpS, x, ty, hpCol, cu); x += hW + 5; }
        if (dW > 0) text(c, m, dS, x, ty, ThemeManager.argb(t.textDim, 1f), cu);
        // held items either side of the name row
        int iy = -rowH / 2 - 8;
        if (m.showOffhand.get()) item(c, m, p.getOffHandStack(), x0 - 18, iy);
        if (m.showMain.get()) item(c, m, p.getMainHandStack(), x0 + w + 2, iy);
        // armor row above
        if (m.showArmor.get()) {
            int ax = -(4 * 17) / 2, ay = -rowH - 19;
            for (int i = 0; i < 4; i++) {
                ItemStack st = p.getEquippedStack(ARMOR[i]); if (st.isEmpty()) continue;
                ItemRenderUtils.drawStack(c, st, ax + i * 17, ay, false, m.showDurability.get());
                if (m.showEnchants.get()) ItemRenderUtils.drawEnchants(c, st, ax + i * 17 + 8, ay - 1, cu);
            }
        }
        c.getMatrices().popMatrix();
    }
    private static void item(DrawContext c, NameTagsModule m, ItemStack st, int x, int y) { if (!st.isEmpty()) ItemRenderUtils.drawStack(c, st, x, y, m.showCount.get(), m.showDurability.get()); }
    private static void text(DrawContext c, NameTagsModule m, String s, int x, int y, int col, boolean cu) {
        if (m.textOutline.get()) FontManager.drawOutlined(c, s, x, y, col, cu); else FontManager.draw(c, s, x, y, col, cu);
    }
}
