package com.mealsnow.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findByStatus(VendorStatus status);

    Page<Vendor> findByStatus(VendorStatus status, Pageable pageable);

}
