package dev.draxon.module.render;
import dev.draxon.core.DraxonClient;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.*;
import net.minecraft.client.MinecraftClient;
/** Client-side visual replacement of the local username in Draxon's own HUD/NameTags. Never touches session, auth or network. */
public class NameProtectModule extends Module {
    public final StringSetting name = str("Replacement", "Protected", 16);
    public final BooleanSetting inHud = bool("Replace in HUD", true), inTags = bool("Replace in NameTags", true);
    public NameProtectModule() { super("Name Protect", Category.RENDER); }
    private static String apply(String s, boolean tags) {
        NameProtectModule m = DraxonClient.modules == null ? null : DraxonClient.modules.get(NameProtectModule.class);
        if (m == null || !m.isEnabled() || !(tags ? m.inTags : m.inHud).get()) return s;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return s;
        String real = mc.player.getName().getString();
        return real.isEmpty() ? s : s.replace(real, m.name.get());
    }
    public static String forHud(String s) { return apply(s, false); }
    public static String forTags(String s) { return apply(s, true); }
}
