package com.ecommerce.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-memory event bus (simulates Kafka).
 * Uses an ordered, sequential dispatch: failure stops the chain.
 * Observer pattern: maintains a registry of listeners per EventType.
 */
@Component
public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final Map<EventType, List<EventListener>> listeners = new EnumMap<>(EventType.class);
    private final BlockingQueue<DomainEvent> eventQueue = new LinkedBlockingQueue<>();
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EventBus-Dispatcher");
        t.setDaemon(true);
        return t;
    });

    public EventBus() {
        startDispatcher();
    }

    private void startDispatcher() {
        dispatcher.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DomainEvent event = eventQueue.take();
                    dispatch(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("EventBus dispatcher error", e);
                }
            }
        });
    }

    public void subscribe(EventType type, EventListener listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
        log.debug("Subscribed {} to {}", listener.getClass().getSimpleName(), type);
    }

    public void publish(DomainEvent event) {
        log.info("[EVENT] Publishing: {} | entity={} | user={}", event.getType(), event.getEntityId(), event.getUserId());
        eventQueue.offer(event);
    }

    public void publishSync(DomainEvent event) {
        log.info("[EVENT-SYNC] Processing: {} | entity={} | user={}", event.getType(), event.getEntityId(), event.getUserId());
        dispatch(event);
    }

    private void dispatch(DomainEvent event) {
        List<EventListener> handlers = listeners.getOrDefault(event.getType(), List.of());
        for (EventListener handler : handlers) {
            try {
                handler.onEvent(event);
            } catch (Exception e) {
                log.error("[EVENT] Handler {} failed for event {}. Chain STOPPED: {}",
                        handler.getClass().getSimpleName(), event.getType(), e.getMessage());
                break; // Rule: failure stops the chain
            }
        }
        event.setProcessed(true);
    }

    public void shutdown() {
        dispatcher.shutdownNow();
    }
}
