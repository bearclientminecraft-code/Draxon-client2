package dev.draxon.module.render;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.*;
public class NameTagsModule extends Module {
    public final BooleanSetting showName = bool("Show Name", true), showHealth = bool("Show Health", true), showDistance = bool("Show Distance", false),
        showArmor = bool("Show Armor", true), showEnchants = bool("Show Enchantments", false), showOffhand = bool("Show Offhand", true),
        showMain = bool("Show Main Hand", true), showDurability = bool("Show Durability", true), showCount = bool("Show Stack Count", true),
        background = bool("Background", true), textOutline = bool("Text Outline", false), customFont = bool("Custom Font", true),
        healthColor = bool("Health Color", true), dynamicScale = bool("Dynamic Scaling", true), friendIndicator = bool("Friend Indicator", true);
    public final NumberSetting blur = num("Blur", 0.5, 0, 1, 0.05), scale = num("Scale", 1.0, 0.5, 2.0, 0.05), vOffset = num("Vertical Offset", 0, -20, 40, 1),
        range = num("Rendering Distance", 64, 8, 128, 4), bgOpacity = num("Background Opacity", 0.7, 0.1, 1, 0.05);
    public final ColorSetting friendColor = color("Friend Color", 0x5AC8FF);
    public NameTagsModule() { super("NameTags", Category.RENDER); }
}
