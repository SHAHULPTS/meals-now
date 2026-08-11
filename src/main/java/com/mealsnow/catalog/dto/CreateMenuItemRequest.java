package com.mealsnow.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateMenuItemRequest(
        @NotBlank(message = "item name is required") String name,
        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero") BigDecimal price,
        String description,   // optional — no annotation
        String category       // optional
) {}