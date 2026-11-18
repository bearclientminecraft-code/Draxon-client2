package dev.draxon.core;

import com.google.gson.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.nio.file.*;
import java.util.*;

/** Friend list (config/draxon/friends.json). Friends are never targeted by combat modules and are what Auto Accept's "Only Friends" checks. */
public final class FriendManager {
    private static final Path FILE = ConfigManager.DIR.resolve("friends.json");
    private static final Map<String, String> FRIENDS = new TreeMap<>(); // lower-case name -> name as typed
    static { load(); }
    private FriendManager() {}

    public static boolean validName(String n) { return n != null && n.matches("[A-Za-z0-9_]{1,16}"); }
    public static boolean add(String n) { if (!validName(n)) return false; boolean fresh = FRIENDS.put(n.toLowerCase(), n) == null; save(); return fresh; }
    public static boolean remove(String n) { if (n == null) return false; boolean had = FRIENDS.remove(n.toLowerCase()) != null; if (had) save(); return had; }
    public static boolean isFriend(String n) { return n != null && FRIENDS.containsKey(n.toLowerCase()); }
    public static boolean isFriend(Entity e) { return e instanceof PlayerEntity && FRIENDS.containsKey(e.getName().getString().toLowerCase()); }
    public static List<String> list() { return new ArrayList<>(FRIENDS.values()); }

    private static void save() {
        try { Files.createDirectories(ConfigManager.DIR); JsonArray a = new JsonArray(); FRIENDS.values().forEach(a::add); Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(a)); }
        catch (Exception ex) { Draxon.LOGGER.error("Failed to save friends", ex); }
    }
    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            for (JsonElement e : JsonParser.parseString(Files.readString(FILE)).getAsJsonArray()) { String n = e.getAsString(); if (validName(n)) FRIENDS.put(n.toLowerCase(), n); }
        } catch (Exception ex) { Draxon.LOGGER.error("Failed to load friends", ex); }
    }
}
