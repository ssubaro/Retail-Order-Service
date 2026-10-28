package com.axity.course.retailorderservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.axity.course.retailorderservice.dto.AuditLogResponseDto;
import com.axity.course.retailorderservice.service.AuditLogService;

/**
 * Expone la evidencia que dejan las tareas asíncronas (notificación, auditoría
 * de almacén y alertas de reabastecimiento) para que pueda verificarse desde
 * Swagger UI o Postman que el procesamiento en segundo plano realmente ocurrió.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLogResponseDto> findAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId) {
        if (entityType != null && entityId != null) {
            return auditLogService.findByEntity(entityType, entityId);
        }
        return auditLogService.findAll();
    }
}
