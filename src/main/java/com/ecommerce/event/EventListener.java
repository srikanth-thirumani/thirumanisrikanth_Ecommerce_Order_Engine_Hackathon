package com.ecommerce.event;

/**
 * Observer pattern: EventListener interface.
 * All event handlers must implement this.
 */
public interface EventListener {
    void onEvent(DomainEvent event);
    boolean supports(EventType type);
}
