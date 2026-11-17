package dev.draxon.render;
import dev.draxon.setting.StringSetting;
import java.util.*;
public final class FriendManager {
    private static StringSetting source; private static String last = ""; private static final Set<String> set = new HashSet<>();
    private FriendManager() {}
    public static void bind(StringSetting s) { source = s; }
    public static boolean isFriend(String name) {
        if (source == null) return false;
        String v = source.get();
        if (!v.equals(last)) { last = v; set.clear(); for (String p : v.split(",")) { p = p.trim().toLowerCase(Locale.ROOT); if (!p.isEmpty()) set.add(p); } }
        return set.contains(name.toLowerCase(Locale.ROOT));
    }
}
