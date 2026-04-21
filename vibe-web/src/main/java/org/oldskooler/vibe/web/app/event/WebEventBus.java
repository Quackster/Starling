package org.oldskooler.vibe.web.app.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class WebEventBus {

    private final Map<Class<?>, List<Consumer<?>>> listenersByType = new LinkedHashMap<>();

    /**
     * Registers a listener for the given event type.
     * @param eventType the event type
     * @param listener the listener
     * @param <T> the event type
     */
    public synchronized <T> void register(Class<T> eventType, Consumer<T> listener) {
        listenersByType.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(listener);
    }

    /**
     * Publishes an event to all listeners for the event class.
     * @param event the event payload
     * @param <T> the event type
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> void publish(T event) {
        if (event == null) {
            return;
        }

        List<Consumer<?>> listeners = listenersByType.getOrDefault(event.getClass(), List.of());
        for (Consumer<?> listener : listeners) {
            ((Consumer<T>) listener).accept(event);
        }
    }
}
