package com.axity.course.retailorderservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequestDto(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @PositiveOrZero Integer stock,
        @Size(max = 50) String category) {
}
