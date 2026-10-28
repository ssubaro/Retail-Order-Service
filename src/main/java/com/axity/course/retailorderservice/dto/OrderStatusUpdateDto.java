package com.axity.course.retailorderservice.dto;

import jakarta.validation.constraints.NotNull;

import com.axity.course.retailorderservice.model.OrderStatus;

public record OrderStatusUpdateDto(@NotNull OrderStatus status) {
}
