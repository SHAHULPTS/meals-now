package com.mealsnow.catalog;

import com.mealsnow.vendor.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
}
