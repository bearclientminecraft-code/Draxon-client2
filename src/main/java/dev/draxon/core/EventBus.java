package dev.draxon.core;
import java.util.*;
import java.util.function.Consumer;
/** Minimal synchronous event bus (client thread only). */
public final class EventBus {
    public static final EventBus INSTANCE = new EventBus();
    private final Map<Class<?>, List<Consumer<Object>>> handlers = new HashMap<>();
    private EventBus() {}
    public <E> void subscribe(Class<E> type, Consumer<E> handler) {
        handlers.computeIfAbsent(type, k -> new ArrayList<>()).add(o -> handler.accept(type.cast(o)));
    }
    public void post(Object event) {
        List<Consumer<Object>> l = handlers.get(event.getClass());
        if (l != null) for (int i = 0; i < l.size(); i++) l.get(i).accept(event);
    }
}
