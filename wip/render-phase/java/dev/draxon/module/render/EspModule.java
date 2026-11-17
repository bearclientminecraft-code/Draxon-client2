package dev.draxon.module.render;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.render.FriendManager;
import dev.draxon.setting.*;
public class EspModule extends Module {
    public final BooleanSetting players = bool("Players", true), hostile = bool("Hostile Mobs", true), passive = bool("Passive Mobs", false),
        animals = bool("Animals", false), friends = bool("Friends", true), healthBar = bool("Health Bar", true), distance = bool("Distance Display", true),
        healthColor = bool("Health-based Color", false);
    public final ModeSetting mode = mode("Mode", "2D Box", "2D Box", "3D Box", "Corner Box", "Outline");
    public final ModeSetting visibility = mode("Visibility", "Through Walls", "Through Walls", "Visible Only");
    public final NumberSetting thickness = num("Line Thickness", 1, 1, 4, 1), opacity = num("Opacity", 0.9, 0.1, 1, 0.05),
        bgOpacity = num("Background Opacity", 0.12, 0, 0.6, 0.02), range = num("Range", 64, 8, 128, 4);
    public final ColorSetting playerColor = color("Player Color", 0xB06BFF), hostileColor = color("Hostile Color", 0xFF5A5A),
        passiveColor = color("Passive Color", 0xF2D45A), animalColor = color("Animal Color", 0x6BE39A), friendColor = color("Friend Color", 0x5AC8FF);
    public final StringSetting friendList = str("Friend List", "", 200);
    public EspModule() { super("ESP", Category.RENDER); FriendManager.bind(friendList); }
}
