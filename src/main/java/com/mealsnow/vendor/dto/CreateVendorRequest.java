package com.mealsnow.vendor.dto;
import jakarta.validation.constraints.NotBlank;

public record CreateVendorRequest (
    @NotBlank(message = "vendor name is required") String name,
    @NotBlank(message = "address is required") String address)
{}



