package com.mealsnow.catalog;

import com.mealsnow.vendor.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByVendorId(UUID vendorId);
    Page<MenuItem> findByVendorIdAndAvailableTrue(UUID vendorId, Pageable pageable);



}
