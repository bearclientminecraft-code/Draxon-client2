package dev.draxon.module.misc;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.*;
public class HudModule extends Module {
    public final NumberSetting scale = num("HUD Scale", 1.0, 0.6, 1.6, 0.05);
    // watermark
    public final LabelSetting l1 = add(new LabelSetting("WATERMARK"));
    public final BooleanSetting watermark = bool("Watermark", true), showName = bool("Show Client Name", true), showVersion = bool("Show Version", true),
        showFps = bool("Show FPS", true), showPing = bool("Show Ping", true), showPlayer = bool("Show Player Name", true), showServer = bool("Show Server", false),
        showTime = bool("Show Time", true), showCoords = bool("Show Coordinates", true), showBps = bool("Show BPS", true), icons = bool("Icons", true),
        background = bool("Background", true), border = bool("Border", true), shadow = bool("Shadow", true), bossAdapt = bool("Boss Bar Adaptation", true);
    // potion list
    public final LabelSetting l2 = add(new LabelSetting("POTION LIST"));
    public final BooleanSetting potionList = bool("Potion List", true);
    public final NumberSetting potionX = num("PotionList X", 0.01, 0, 1, 0.001), potionY = num("PotionList Y", 0.22, 0, 1, 0.001);
    // keybinds
    public final LabelSetting l3 = add(new LabelSetting("KEYBINDS"));
    public final BooleanSetting keybinds = bool("KeyBinds", true), keybindsAll = bool("Show Disabled Binds", false);
    public final NumberSetting keysX = num("KeyBinds X", 0.01, 0, 1, 0.001), keysY = num("KeyBinds Y", 0.45, 0, 1, 0.001);
    // notifications
    public final LabelSetting l4 = add(new LabelSetting("NOTIFICATIONS"));
    public final BooleanSetting notifications = bool("Notifications", true);
    public final NumberSetting notifDuration = num("Duration", 3, 1, 10, 0.5), notifOffset = num("Offset", 24, 0, 120, 1), notifSpacing = num("Spacing", 4, 0, 14, 1), notifMax = num("Max Count", 4, 1, 8, 1);
    public HudModule() {
        super("Hud", Category.MISC);
        for (NumberSetting n : new NumberSetting[]{potionX, potionY, keysX, keysY}) n.visibleIf(() -> false); // positions are set by dragging in chat
        setDefaultEnabled(true);
    }
}
