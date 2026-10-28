package com.axity.course.retailorderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponseDto(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
