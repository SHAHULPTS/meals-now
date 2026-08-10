package com.mealsnow.order;

import com.mealsnow.vendor.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
