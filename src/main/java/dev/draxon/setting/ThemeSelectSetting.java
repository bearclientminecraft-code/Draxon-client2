package dev.draxon.setting;
import dev.draxon.gui.Theme;
public class ThemeSelectSetting extends Setting<String> {
    public static final String CUSTOM = "Custom";
    public ThemeSelectSetting(String n, String def) { super(n, def); }
    @Override public void set(String v) { if (CUSTOM.equals(v) || Theme.find(v) != null) value = v; }
    public boolean isCustom() { return CUSTOM.equals(value); }
    @Override public String serialize() { return value; }
    @Override public void deserialize(String s) { set(s); }
}
