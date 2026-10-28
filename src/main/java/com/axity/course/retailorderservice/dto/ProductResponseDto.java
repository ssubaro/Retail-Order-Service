package com.axity.course.retailorderservice.dto;

import java.math.BigDecimal;

public record ProductResponseDto(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String category,
        boolean active) {
}
