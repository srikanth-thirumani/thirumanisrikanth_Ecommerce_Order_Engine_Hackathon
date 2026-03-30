package com.ecommerce.event;

import com.ecommerce.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Observer: Listens to INVENTORY_UPDATED events.
 */
@Component
public class InventoryUpdatedListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryUpdatedListener.class);

    private final EventBus eventBus;
    private final AuditLogger auditLogger;

    public InventoryUpdatedListener(EventBus eventBus, AuditLogger auditLogger) {
        this.eventBus = eventBus;
        this.auditLogger = auditLogger;
    }

    @PostConstruct
    public void register() {
        eventBus.subscribe(EventType.INVENTORY_UPDATED, this);
        eventBus.subscribe(EventType.INVENTORY_RESERVED, this);
        eventBus.subscribe(EventType.INVENTORY_RELEASED, this);
        eventBus.subscribe(EventType.RESERVATION_EXPIRED, this);
    }

    @Override
    public void onEvent(DomainEvent event) {
        log.info("[EVENT-HANDLER] {}: product={} user={} details={}",
                event.getType(), event.getEntityId(), event.getUserId(), event.getPayload());

        if (event.getType() == EventType.RESERVATION_EXPIRED) {
            auditLogger.logSystem("RESERVATION_AUTO_RELEASED", "PRODUCT", event.getEntityId(),
                    "user=" + event.getUserId() + " qty=" + event.getPayload().get("qty"));
        }
    }

    @Override
    public boolean supports(EventType type) {
        return type == EventType.INVENTORY_UPDATED
                || type == EventType.INVENTORY_RESERVED
                || type == EventType.INVENTORY_RELEASED
                || type == EventType.RESERVATION_EXPIRED;
    }
}
