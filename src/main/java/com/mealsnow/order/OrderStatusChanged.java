package com.mealsnow.order;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChanged(UUID orderId, UUID customerId, UUID vendorId,
                                 OrderStatus oldStatus, OrderStatus newStatus, Instant occurredAt) {
}
