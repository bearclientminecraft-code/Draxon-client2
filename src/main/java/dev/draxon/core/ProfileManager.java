package dev.draxon.core;

import com.google.gson.*;
import dev.draxon.module.Module;
import dev.draxon.module.ModuleManager;
import dev.draxon.setting.LabelSetting;
import dev.draxon.setting.Setting;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Named config profiles (config/draxon/profiles/<name>.json, same format as modules.json) and factory defaults. */
public final class ProfileManager {
    public static final Path DIR = ConfigManager.DIR.resolve("profiles");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Module, Map<Setting<?>, String>> DEFAULTS = new HashMap<>();
    private static final Map<Module, Boolean> DEFAULT_ENABLED = new HashMap<>();
    private ProfileManager() {}

    /** Call after all modules are registered and BEFORE the config is loaded. */
    public static void captureDefaults(ModuleManager mm) {
        for (Module m : mm.all()) {
            Map<Setting<?>, String> d = new HashMap<>();
            for (Setting<?> s : m.settings) if (!(s instanceof LabelSetting)) d.put(s, s.serialize());
            DEFAULTS.put(m, d); DEFAULT_ENABLED.put(m, m.isEnabled());
        }
    }
    public static boolean validName(String n) { return n != null && n.matches("[A-Za-z0-9_-]{1,32}"); }

    public static boolean save(String name, ModuleManager mm) {
        if (!validName(name)) return false;
        try { Files.createDirectories(DIR); Files.writeString(DIR.resolve(name + ".json"), GSON.toJson(ConfigManager.exportModules(mm))); return true; }
        catch (Exception ex) { Draxon.LOGGER.error("Failed to save profile " + name, ex); return false; }
    }
    public static boolean load(String name, ModuleManager mm) {
        if (!validName(name)) return false;
        try {
            Path p = DIR.resolve(name + ".json");
            if (!Files.exists(p)) return false;
            JsonElement e = JsonParser.parseString(Files.readString(p));
            if (!e.isJsonObject()) return false;
            ConfigManager.importModules(mm, e.getAsJsonObject()); ConfigManager.markDirty(); return true;
        } catch (Exception ex) { Draxon.LOGGER.error("Failed to load profile " + name, ex); return false; }
    }
    public static boolean delete(String name) {
        if (!validName(name)) return false;
        try { return Files.deleteIfExists(DIR.resolve(name + ".json")); } catch (Exception ex) { return false; }
    }
    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.exists(DIR) ? Files.list(DIR) : Stream.empty()) {
            s.map(p -> p.getFileName().toString()).filter(f -> f.endsWith(".json")).forEach(f -> out.add(f.substring(0, f.length() - 5)));
        } catch (Exception ignored) {}
        Collections.sort(out); return out;
    }
    /** Restores every setting (keybinds included) and the enabled state to the values captured at startup. */
    public static void resetToDefaults(ModuleManager mm) {
        for (Module m : mm.all()) {
            Map<Setting<?>, String> d = DEFAULTS.get(m);
            if (d != null) for (Map.Entry<Setting<?>, String> en : d.entrySet()) { try { en.getKey().deserialize(en.getValue()); } catch (RuntimeException ignored) {} }
            m.validate();
            m.setEnabled(DEFAULT_ENABLED.getOrDefault(m, false));
        }
        ConfigManager.markDirty();
    }
}
