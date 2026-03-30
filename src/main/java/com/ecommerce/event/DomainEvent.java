package com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class DomainEvent {

    private String eventId;
    private EventType type;
    private String userId;
    private String entityId;
    private String entityType;
    private Map<String, Object> payload;
    private LocalDateTime occurredAt;
    private boolean processed;

    public DomainEvent() {}

    public static DomainEvent of(EventType type, String userId, String entityId, String entityType, Map<String, Object> payload) {
        DomainEvent e = new DomainEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setType(type);
        e.setUserId(userId);
        e.setEntityId(entityId);
        e.setEntityType(entityType);
        e.setPayload(payload);
        e.setOccurredAt(LocalDateTime.now());
        e.setProcessed(false);
        return e;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public static DomainEventBuilder builder() {
        return new DomainEventBuilder();
    }

    public static class DomainEventBuilder {
        private String eventId;
        private EventType type;
        private String userId;
        private String entityId;
        private String entityType;
        private Map<String, Object> payload;
        private LocalDateTime occurredAt;
        private boolean processed;

        public DomainEventBuilder eventId(String eventId) { this.eventId = eventId; return this; }
        public DomainEventBuilder type(EventType type) { this.type = type; return this; }
        public DomainEventBuilder userId(String userId) { this.userId = userId; return this; }
        public DomainEventBuilder entityId(String entityId) { this.entityId = entityId; return this; }
        public DomainEventBuilder entityType(String entityType) { this.entityType = entityType; return this; }
        public DomainEventBuilder payload(Map<String, Object> payload) { this.payload = payload; return this; }
        public DomainEventBuilder occurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; return this; }
        public DomainEventBuilder processed(boolean processed) { this.processed = processed; return this; }

        public DomainEvent build() {
            DomainEvent e = new DomainEvent();
            e.setEventId(eventId);
            e.setType(type);
            e.setUserId(userId);
            e.setEntityId(entityId);
            e.setEntityType(entityType);
            e.setPayload(payload);
            e.setOccurredAt(occurredAt);
            e.setProcessed(processed);
            return e;
        }
    }
}
