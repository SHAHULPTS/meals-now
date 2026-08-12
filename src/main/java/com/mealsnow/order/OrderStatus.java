package com.mealsnow.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PLACED, ACCEPTED, PREPARING, READY,
    OUT_FOR_DELIVERY, DELIVERED,
    CANCELLED, REJECTED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);
    static {
        ALLOWED.put(PLACED,           EnumSet.of(ACCEPTED, REJECTED, CANCELLED));
        ALLOWED.put(ACCEPTED,         EnumSet.of(PREPARING, CANCELLED));
        ALLOWED.put(PREPARING,        EnumSet.of(READY, CANCELLED));
        ALLOWED.put(READY,            EnumSet.of(OUT_FOR_DELIVERY));
        ALLOWED.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED));
        ALLOWED.put(DELIVERED,        EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(CANCELLED,        EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(REJECTED,         EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}
