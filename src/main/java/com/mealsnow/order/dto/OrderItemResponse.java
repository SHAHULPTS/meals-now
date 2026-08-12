package com.mealsnow.order.dto;

import com.mealsnow.order.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        String itemName,
        BigDecimal unitprice,
        int quantity
) {
    public static OrderItemResponse from(OrderItem oi) {
        return new OrderItemResponse(oi.getItemName(), oi.getUnitPrice(), oi.getQuantity());
    }
}
