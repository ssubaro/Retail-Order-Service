package com.axity.course.retailorderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.axity.course.retailorderservice.model.OrderStatus;

public record OrderResponseDto(
        Long id,
        String customerId,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        List<OrderItemResponseDto> items) {
}
