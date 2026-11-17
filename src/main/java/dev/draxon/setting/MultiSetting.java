package dev.draxon.setting;
import java.util.*;
public class MultiSetting extends Setting<Set<String>> {
    public final String[] options;
    public MultiSetting(String n, String[] options, String... selected) { super(n, new LinkedHashSet<>(Arrays.asList(selected))); this.options = options; }
    public boolean has(String o) { return value.contains(o); }
    public void toggle(String o) { if (!value.remove(o)) value.add(o); }
    @Override public String serialize() { return String.join(",", value); }
    @Override public void deserialize(String s) {
        value.clear();
        if (s.isEmpty()) return;
        for (String p : s.split(",")) for (String o : options) if (o.equals(p)) value.add(o);
    }
}
