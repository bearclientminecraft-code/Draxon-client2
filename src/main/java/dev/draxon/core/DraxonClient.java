package dev.draxon.core;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.ThemeManager;
import dev.draxon.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public class DraxonClient implements ClientModInitializer {
    public static ModuleManager modules; public static ThemeManager theme; public static KeybindManager keybinds;
    @Override public void onInitializeClient() {
        ConfigManager.init();
        FontManager.init();
        theme = new ThemeManager(); ConfigManager.loadTheme(theme);
        modules = new ModuleManager(); ConfigManager.loadModules(modules);
        keybinds = new KeybindManager(modules);
        int[] ticks = {0}; // autosave every 10 s if something changed
        EventBus.INSTANCE.subscribe(Events.Tick.class, e -> { if (++ticks[0] >= 200) { ticks[0] = 0; if (ConfigManager.isDirty()) ConfigManager.saveAll(theme, modules); } });
        Draxon.LOGGER.info("{} initialised with {} modules", Draxon.NAME, modules.all().size());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> EventBus.INSTANCE.post(new Events.Tick(mc)));
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigManager.saveAll(theme, modules));
    }
}
