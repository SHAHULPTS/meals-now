package com.mealsnow.order;

import com.mealsnow.vendor.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByVendorOwnerId(UUID ownerId, Pageable pageable);
}
