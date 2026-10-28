package com.axity.course.retailorderservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record OrderRequestDto(
        @NotBlank @Size(max = 50) String customerId,
        @NotEmpty @Valid List<OrderItemRequestDto> items) {
}
