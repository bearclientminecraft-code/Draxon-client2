package dev.draxon.setting;
/** Section heading inside a module's settings. Not persisted. */
public class LabelSetting extends Setting<String> {
    public LabelSetting(String n) { super(n, n); }
    @Override public String serialize() { return ""; }
    @Override public void deserialize(String s) {}
}
