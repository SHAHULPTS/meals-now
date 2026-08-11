package com.mealsnow.vendor;

import com.mealsnow.vendor.dto.VendorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/vendors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVendorController {
    private final VendorService vendorService;
    public AdminVendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }


    @GetMapping
    public ResponseEntity<List<VendorResponse>> listByStatus
            (@RequestParam VendorStatus status){
        List<VendorResponse> vendors = vendorService.listByStatus(status);

        return ResponseEntity.ok(vendors);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<VendorResponse> approve(@PathVariable UUID id) {
        VendorResponse response = vendorService.approve(id);
        return ResponseEntity.ok(response);

    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<VendorResponse> suspend(
            @PathVariable UUID id) {

        VendorResponse response = vendorService.suspend(id);

        return ResponseEntity.ok(response);
    }



}
