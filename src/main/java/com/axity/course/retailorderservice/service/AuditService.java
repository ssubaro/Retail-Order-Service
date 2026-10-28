package com.axity.course.retailorderservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axity.course.retailorderservice.model.AuditLog;
import com.axity.course.retailorderservice.repository.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String entityType, Long entityId, String action, String message) {
        auditLogRepository.save(new AuditLog(entityType, entityId, action, message));
    }
}
