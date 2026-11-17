package dev.draxon.core;
import dev.draxon.gui.FontManager;
import dev.draxon.gui.ThemeManager;
import dev.draxon.hud.HudManager;
import dev.draxon.module.ModuleManager;
import dev.draxon.module.combat.*;
import dev.draxon.module.movement.*;
import dev.draxon.module.misc.HudModule;
import dev.draxon.module.misc.ThemesModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public class DraxonClient implements ClientModInitializer {
    public static ModuleManager modules; public static ThemeManager theme; public static KeybindManager keybinds;
    @Override public void onInitializeClient() {
        ConfigManager.init();
        FontManager.init();
        theme = new ThemeManager(); ConfigManager.loadTheme(theme);
        modules = new ModuleManager();
        modules.register(new ThemesModule(theme)); modules.register(new HudModule());
        // Combat / Sword (order matters: tap modules before TargetStrafe/AutoSprint so the movement lock is taken first)
        modules.register(new TargetsModule());
        modules.register(new TriggerBot()); modules.register(new AimAssist()); modules.register(new AutoClicker());
        modules.register(new WTap()); modules.register(new STap()); modules.register(new SprintReset());
        modules.register(new Velocity()); modules.register(new HitSelect()); modules.register(new TargetStrafe());
        modules.register(new ShieldDisabler()); modules.register(new AutoSprint());
        modules.register(new Sprint()); modules.register(new NoJumpDelay());
        ProfileManager.captureDefaults(modules);
        ConfigManager.loadModules(modules);
        HudManager.init();
        keybinds = new KeybindManager(modules);
        int[] ticks = {0}; // autosave every 10 s if something changed
        EventBus.INSTANCE.subscribe(Events.Tick.class, e -> { if (++ticks[0] >= 200) { ticks[0] = 0; if (ConfigManager.isDirty()) ConfigManager.saveAll(theme, modules); } });
        Draxon.LOGGER.info("{} initialised with {} modules", Draxon.NAME, modules.all().size());
        ClientTickEvents.START_CLIENT_TICK.register(mc -> EventBus.INSTANCE.post(new Events.PreTick(mc)));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> EventBus.INSTANCE.post(new Events.Tick(mc)));
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((mc, world) -> EventBus.INSTANCE.post(new Events.Reset()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> EventBus.INSTANCE.post(new Events.Reset()));
        Commands.init();
        dev.draxon.combat.RotationManager.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigManager.saveAll(theme, modules));
    }
}
