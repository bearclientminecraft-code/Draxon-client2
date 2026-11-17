package dev.draxon.module;
import dev.draxon.combat.CombatState;
import dev.draxon.combat.RotationManager;
import dev.draxon.core.Draxon;
import dev.draxon.core.EventBus;
import dev.draxon.core.Events;
import java.util.*;
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    public ModuleManager() {
        EventBus.INSTANCE.subscribe(Events.PreTick.class, e -> {
            CombatState.update(e.client());
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                if (!m.isEnabled()) continue;
                try { m.onPreTick(e.client()); } catch (RuntimeException ex) { Draxon.LOGGER.error("Module " + m.name + " threw in onPreTick", ex); }
            }
        });
        EventBus.INSTANCE.subscribe(Events.Tick.class, e -> {
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                if (!m.isEnabled()) continue;
                try { m.onTick(e.client()); } catch (RuntimeException ex) { Draxon.LOGGER.error("Module " + m.name + " threw in onTick", ex); }
            }
            RotationManager.endTick(); // goal lifetime; the view itself moves per frame
        });
        EventBus.INSTANCE.subscribe(Events.Reset.class, e -> {
            for (Module m : modules) { try { m.reset(); } catch (RuntimeException ex) { Draxon.LOGGER.error("Module " + m.name + " threw in reset", ex); } }
            CombatState.reset(); RotationManager.clear();
        });
    }
    public void register(Module m) {
        for (Module o : modules) if (o.name.equals(m.name)) throw new IllegalArgumentException("Duplicate module name: " + m.name);
        modules.add(m);
    }
    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) { for (Module m : modules) if (type.isInstance(m)) return (T) m; return null; }
    public List<Module> all() { return modules; }
    public List<Module> in(Category c) { List<Module> l = new ArrayList<>(); for (Module m : modules) if (m.category == c) l.add(m); return l; }
}
