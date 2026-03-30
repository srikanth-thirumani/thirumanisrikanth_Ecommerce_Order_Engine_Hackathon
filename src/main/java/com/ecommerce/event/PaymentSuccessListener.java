package com.ecommerce.event;

import com.ecommerce.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Observer: Listens to PAYMENT_SUCCESS events.
 * Triggers shipment initiation (simulated).
 */
@Component
public class PaymentSuccessListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSuccessListener.class);

    private final EventBus eventBus;
    private final AuditLogger auditLogger;

    public PaymentSuccessListener(EventBus eventBus, AuditLogger auditLogger) {
        this.eventBus = eventBus;
        this.auditLogger = auditLogger;
    }

    @PostConstruct
    public void register() {
        eventBus.subscribe(EventType.PAYMENT_SUCCESS, this);
    }

    @Override
    public void onEvent(DomainEvent event) {
        String orderId = event.getEntityId();
        String txn = (String) event.getPayload().getOrDefault("txn", "N/A");
        log.info("[EVENT-HANDLER] PAYMENT_SUCCESS: orderId={} txn={} user={}",
                orderId, txn, event.getUserId());
        // Simulate shipment initiation
        auditLogger.logSystem("SHIPMENT_INITIATED", "ORDER", orderId,
                String.format("txn=%s user=%s", txn, event.getUserId()));
    }

    @Override
    public boolean supports(EventType type) {
        return type == EventType.PAYMENT_SUCCESS;
    }
}
