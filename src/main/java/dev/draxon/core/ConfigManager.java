package dev.draxon.core;
import com.google.gson.*;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.Module;
import dev.draxon.module.ModuleManager;
import dev.draxon.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.List;
public final class ConfigManager {
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("draxon");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean dirty;
    private ConfigManager() {}
    public static void init() { EventBus.INSTANCE.subscribe(Events.ModuleToggle.class, e -> dirty = true); }
    public static void markDirty() { dirty = true; }

    public static void writeSettings(JsonObject o, List<Setting<?>> l) { for (Setting<?> s : l) if (!(s instanceof dev.draxon.setting.LabelSetting)) o.addProperty(s.name, s.serialize()); }
    public static void readSettings(JsonObject o, List<Setting<?>> l) {
        for (Setting<?> s : l) if (o.has(s.name)) { try { s.deserialize(o.get(s.name).getAsString()); } catch (RuntimeException ignored) {} }
    }
    private static void write(String file, JsonObject o) {
        try {
            Files.createDirectories(DIR);
            Path tmp = DIR.resolve(file + ".tmp");
            Files.writeString(tmp, GSON.toJson(o));
            Files.move(tmp, DIR.resolve(file), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) { Draxon.LOGGER.error("Failed to save " + file, ex); }
    }
    private static JsonObject read(String file) {
        try { Path p = DIR.resolve(file); if (Files.exists(p)) return JsonParser.parseString(Files.readString(p)).getAsJsonObject(); }
        catch (Exception ex) { Draxon.LOGGER.error("Failed to read " + file, ex); }
        return null;
    }
    public static void saveTheme(ThemeManager t) { JsonObject o = new JsonObject(); writeSettings(o, t.settings); write("theme.json", o); }
    public static void loadTheme(ThemeManager t) { JsonObject o = read("theme.json"); if (o != null) readSettings(o, t.settings); }
    public static JsonObject exportModules(ModuleManager mm) {
        JsonObject root = new JsonObject();
        for (Module m : mm.all()) { JsonObject o = new JsonObject(); o.addProperty("enabled", m.isEnabled()); writeSettings(o, m.settings); root.add(m.name, o); }
        return root;
    }
    /** Applies a modules JSON (modules.json or a profile). Invalid entries are skipped, values are clamped by the settings, then validate() runs. */
    public static void importModules(ModuleManager mm, JsonObject root) {
        for (Module m : mm.all()) {
            if (!root.has(m.name) || !root.get(m.name).isJsonObject()) continue;
            JsonObject o = root.getAsJsonObject(m.name);
            readSettings(o, m.settings);
            m.validate();
            if (o.has("enabled")) { try { m.setEnabled(o.get("enabled").getAsBoolean()); } catch (RuntimeException ignored) {} }
        }
    }
    public static void saveModules(ModuleManager mm) { write("modules.json", exportModules(mm)); }
    public static void loadModules(ModuleManager mm) { JsonObject root = read("modules.json"); if (root != null) importModules(mm, root); }
    public static void saveAll(ThemeManager t, ModuleManager mm) { saveTheme(t); saveModules(mm); dirty = false; }
    public static boolean isDirty() { return dirty; }
}
