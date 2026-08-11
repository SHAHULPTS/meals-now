package com.mealsnow.catalog.dto;

import com.mealsnow.catalog.MenuItem;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        boolean available,
        String category,
        UUID vendorId
) {
    public static MenuItemResponse from(MenuItem m) {
        return new MenuItemResponse(
                m.getId(),
                m.getName(),
                m.getDescription(),
                m.getPrice(),
                m.isAvailable(),      // boolean getter is isAvailable(), not getAvailable()
                m.getCategory(),
                m.getVendor().getId()
        );
    }
}