package com.mealsnow.vendor;


import com.mealsnow.vendor.dto.CreateVendorRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.mealsnow.vendor.dto.VendorResponse;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorService vendorService;
    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<Page<VendorResponse>> listApproved(Pageable pageable) {
        return ResponseEntity.ok(vendorService.listApproved(pageable));
    }


    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VendorResponse> apply(
            @Valid @RequestBody CreateVendorRequest req,
            @AuthenticationPrincipal String userId){
        VendorResponse response = vendorService.apply(userId, req);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
