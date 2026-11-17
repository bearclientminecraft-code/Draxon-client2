package dev.draxon.setting;
public class StringSetting extends Setting<String> {
    public final int maxLen;
    public StringSetting(String n, String d, int maxLen) { super(n, d); this.maxLen = maxLen; }
    @Override public void set(String v) { value = v.length() > maxLen ? v.substring(0, maxLen) : v; }
    @Override public String serialize() { return value; }
    @Override public void deserialize(String s) { set(s); }
}
