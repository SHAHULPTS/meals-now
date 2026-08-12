package com.mealsnow.order.dto;

import com.mealsnow.order.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID vendorId,
        String status,
        BigDecimal total,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getVendor().getId(), o.getStatus().name(), o.getTotal(),
                o.getItems().stream().map(OrderItemResponse::from).toList());
    }

}
