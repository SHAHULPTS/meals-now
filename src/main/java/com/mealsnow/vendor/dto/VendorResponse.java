package com.mealsnow.vendor.dto;

import com.mealsnow.vendor.Vendor;
import com.mealsnow.vendor.VendorStatus;

import java.util.UUID;

public record VendorResponse(UUID id, String name, String address,
                             VendorStatus status, UUID ownerId) {

    public static VendorResponse from(Vendor v) {
        return new VendorResponse(
                v.getId(),
                v.getName(),
                v.getAddress(),
                v.getStatus(),
                v.getOwner().getId());
    }
}