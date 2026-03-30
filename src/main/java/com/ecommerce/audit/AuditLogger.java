package com.ecommerce.audit;

import com.ecommerce.model.AuditLog;
import com.ecommerce.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Singleton Audit Logger.
 * Spring @Component ensures only one instance exists.
 * Logs are immutable once persisted.
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogRepository auditLogRepository;

    public AuditLogger(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String userId, String action, String entityType, String entityId, String details) {
        LocalDateTime now = LocalDateTime.now();
        String formatted = String.format("[%s] %s %s %s=%s | %s",
                now.format(FORMATTER), userId, action, entityType, entityId, details);

        log.info("[AUDIT] {}", formatted);

        AuditLog entry = AuditLog.builder()
                .timestamp(now)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(entry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSystem(String action, String entityType, String entityId, String details) {
        log("SYSTEM", action, entityType, entityId, details);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int minutes) {
        return auditLogRepository.findRecentLogs(LocalDateTime.now().minusMinutes(minutes));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public String formatLog(AuditLog log) {
        return String.format("[%s] %-12s %-20s %-15s=%s | %s",
                log.getTimestamp().format(FORMATTER),
                log.getUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails());
    }
}
