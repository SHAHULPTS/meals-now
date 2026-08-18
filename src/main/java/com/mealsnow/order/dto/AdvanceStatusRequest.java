package com.mealsnow.order.dto;

import com.mealsnow.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record AdvanceStatusRequest(
        @NotNull OrderStatus target
        ) {
}
