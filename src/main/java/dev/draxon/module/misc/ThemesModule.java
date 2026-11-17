package dev.draxon.module.misc;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.Category;
import dev.draxon.module.Module;
import dev.draxon.setting.*;
/** Theme picker. Disabled -> original Dark Glass palette. Custom shows the Accent and Panel color fields. */
public class ThemesModule extends Module {
    public final ThemeSelectSetting theme = add(new ThemeSelectSetting("Theme", "Sunflower"));
    public final ColorSetting accent = color("Accent", 0x8B5CF6), panel = color("Panel", 0x14141C);
    public ThemesModule(ThemeManager t) {
        super("Themes", Category.MISC);
        accent.visibleIf(theme::isCustom); panel.visibleIf(theme::isCustom);
        setDefaultEnabled(true);
        t.bind(theme, accent, panel, this::isEnabled);
    }
}
