package com.ecommerce.event;

import com.ecommerce.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Observer: Listens to ORDER_CREATED events.
 * Sends order confirmation (simulated).
 */
@Component
public class OrderCreatedListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final EventBus eventBus;
    private final AuditLogger auditLogger;

    public OrderCreatedListener(EventBus eventBus, AuditLogger auditLogger) {
        this.eventBus = eventBus;
        this.auditLogger = auditLogger;
    }

    @PostConstruct
    public void register() {
        eventBus.subscribe(EventType.ORDER_CREATED, this);
    }

    @Override
    public void onEvent(DomainEvent event) {
        log.info("[EVENT-HANDLER] ORDER_CREATED: orderId={} user={} total={}",
                event.getEntityId(), event.getUserId(),
                event.getPayload().getOrDefault("total", "?"));
        // Simulate sending confirmation email
        auditLogger.logSystem("ORDER_CONFIRMATION_SENT", "ORDER", event.getEntityId(),
                "user=" + event.getUserId());
    }

    @Override
    public boolean supports(EventType type) {
        return type == EventType.ORDER_CREATED;
    }
}
