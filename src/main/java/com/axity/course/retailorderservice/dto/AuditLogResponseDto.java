package com.axity.course.retailorderservice.dto;

import java.time.LocalDateTime;

public record AuditLogResponseDto(
        Long id,
        String entityType,
        Long entityId,
        String action,
        String message,
        LocalDateTime createdAt) {
}
