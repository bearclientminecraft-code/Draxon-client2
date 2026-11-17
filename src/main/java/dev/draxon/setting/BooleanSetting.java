package dev.draxon.setting;
public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String n, boolean d) { super(n, d); }
    public void toggle() { value = !value; }
    @Override public String serialize() { return String.valueOf(value); }
    @Override public void deserialize(String s) { value = Boolean.parseBoolean(s); }
}
