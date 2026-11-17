package dev.draxon.module;
import dev.draxon.combat.DelayManager;
import dev.draxon.core.EventBus;
import dev.draxon.core.Events;
import dev.draxon.setting.*;
import net.minecraft.client.MinecraftClient;
import java.util.*;
/** Logic-only base class. No GUI code here. */
public abstract class Module {
    public final String name; public final Category category; public final String sub;
    public final KeybindSetting bind = new KeybindSetting("Bind", -1);
    private boolean enabled; public final List<Setting<?>> settings = new ArrayList<>();
    protected Module(String name, Category c) { this(name, c, ""); }
    protected Module(String name, Category c, String sub) { this.name = name; this.category = c; this.sub = sub; settings.add(bind); }
    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }
    protected BooleanSetting bool(String n, boolean d) { return add(new BooleanSetting(n, d)); }
    protected NumberSetting num(String n, double d, double min, double max, double step) { return add(new NumberSetting(n, d, min, max, step)); }
    protected ModeSetting mode(String n, String d, String... m) { return add(new ModeSetting(n, d, m)); }
    protected ColorSetting color(String n, int rgb) { return add(new ColorSetting(n, rgb)); }
    protected StringSetting str(String n, String d, int max) { return add(new StringSetting(n, d, max)); }
    /** Slider in milliseconds. */
    protected NumberSetting ms(String n, double d, double min, double max) { return add(new NumberSetting(n, d, min, max, 1).unit(" ms")); }
    protected NumberSetting pct(String n, double d, double min, double max) { return add(new NumberSetting(n, d, min, max, 1).unit("%")); }
    /** Delay + Random Delay slider pair (ActualDelay = Base + Random(-R, +R)). */
    protected DelayManager.Delay delay(String name, double def, double max, String rndName, double rndDef, double rndMax) {
        return new DelayManager.Delay(ms(name, def, 0, max), ms(rndName, rndDef, 0, rndMax));
    }
    /** Initial state without firing events (before config load). */
    protected void setDefaultEnabled(boolean e) { enabled = e; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) {
        if (e == enabled) return; enabled = e;
        if (e) onEnable(); else onDisable();
        EventBus.INSTANCE.post(new Events.ModuleToggle(this, e));
    }
    public void toggle() { setEnabled(!enabled); }
    protected void onEnable() {} protected void onDisable() {}
    /** Called every client tick while the module is enabled (driven by ModuleManager via the EventBus). */
    public void onTick(MinecraftClient mc) {}
    /** Called at the START of every client tick while enabled (before input is read). Use for key overrides. */
    public void onPreTick(MinecraftClient mc) {}
    /** World change / disconnect: drop transient state (called for every module, enabled or not). */
    public void reset() {}
    /** Called after a config/profile load: fix inconsistent values (e.g. Min > Max). */
    public void validate() {}
}
