package com.axity.course.retailorderservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axity.course.retailorderservice.dto.AuditLogResponseDto;
import com.axity.course.retailorderservice.model.AuditLog;
import com.axity.course.retailorderservice.repository.AuditLogRepository;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLogResponseDto> findByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLogResponseDto> findAll() {
        return auditLogRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponseDto toResponse(AuditLog log) {
        return new AuditLogResponseDto(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getMessage(),
                log.getCreatedAt());
    }
}
