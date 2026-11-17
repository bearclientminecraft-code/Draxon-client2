package dev.draxon.module.render;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.*;
public class HudModule extends Module {
    public final BooleanSetting watermark = bool("Watermark", true), notifications = bool("Notifications", true),
        showName = bool("Show Client Name", true), showVersion = bool("Show Version", true), showFps = bool("Show FPS", true),
        showPing = bool("Show Ping", true), showPlayer = bool("Show Player Name", true), showServer = bool("Show Server", false),
        showTime = bool("Show Time", true), showCoords = bool("Show Coordinates", true), showBps = bool("Show BPS", true),
        icons = bool("Icons", true), background = bool("Background", true), shadow = bool("Shadow", true), border = bool("Border", true),
        bossAdapt = bool("Boss Bar Adaptation", true);
    public final NumberSetting blur = num("Blur", 0.6, 0, 1, 0.05), opacity = num("Opacity", 0.8, 0.1, 1, 0.01), scale = num("Scale", 1.0, 0.6, 2.0, 0.05),
        padding = num("Padding", 4, 2, 10, 1), offX = num("Offset X", 0, -200, 200, 1), offY = num("Offset Y", 0, 0, 100, 1);
    public final ColorSetting accent = color("Accent Color", 0x79B9F5);
    public final ModeSetting position = mode("Position", "Top Center", "Top Center", "Top Left", "Top Right");
    // notifications
    public final BooleanSetting notifyToggle = bool("Notify on Toggle", true);
    public final ModeSetting notifPos = mode("Notification Position", "Bottom Right", "Bottom Right", "Bottom Left", "Top Right", "Top Left");
    public final NumberSetting notifDuration = num("Notification Duration", 3, 1, 10, 0.5), notifSpacing = num("Notification Spacing", 4, 0, 14, 1),
        notifMax = num("Max Notifications", 5, 1, 8, 1), notifScale = num("Notification Scale", 1.0, 0.6, 1.6, 0.05);
    public final ColorSetting successColor = color("Success Color", 0x6BE39A), warningColor = color("Warning Color", 0xF2B45A);
    public HudModule() { super("HUD", Category.RENDER); }
}
