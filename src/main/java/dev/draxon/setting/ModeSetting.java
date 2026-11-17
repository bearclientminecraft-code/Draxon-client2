package dev.draxon.setting;
public class ModeSetting extends Setting<String> {
    public final String[] modes;
    public ModeSetting(String n, String d, String... modes) { super(n, d); this.modes = modes; }
    @Override public void set(String v) { for (String m : modes) if (m.equals(v)) { value = v; return; } }
    public void cycle(int dir) {
        for (int i = 0; i < modes.length; i++) if (modes[i].equals(value)) { value = modes[((i + dir) % modes.length + modes.length) % modes.length]; return; }
    }
    @Override public String serialize() { return value; }
    @Override public void deserialize(String s) { set(s); }
}
