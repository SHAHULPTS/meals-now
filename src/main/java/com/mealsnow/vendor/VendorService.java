package com.mealsnow.vendor;


import com.mealsnow.identity.User;
import com.mealsnow.identity.UserRepository;
import com.mealsnow.vendor.dto.CreateVendorRequest;
import com.mealsnow.vendor.dto.VendorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.mealsnow.common.error.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class VendorService {
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;


    public VendorService(VendorRepository vendorRepository, UserRepository userRepository) {
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
    }

    public List<VendorResponse> listByStatus(VendorStatus status) {
        return vendorRepository.findByStatus(status)
                .stream().map(VendorResponse::from).toList();
    }

    @Transactional
    public VendorResponse apply(String ownerId, CreateVendorRequest req) {
        // 1. load the User by ownerId (UUID.fromString(ownerId)) from UserRepository
        //    -> if missing, throw for now (Step 3 makes this a clean 404)
        User owner = userRepository.findById(UUID.fromString(ownerId))
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + ownerId));



        // 2. new Vendor()

        Vendor vendor = new Vendor();
        // 3. vendor.setOwner(user)

        vendor.setOwner(owner);
        // 4. vendor.setName(req.name());  setAddress(req.address())
        vendor.setName(req.name());
        vendor.setAddress(req.address());
        // 5. Server decides the initial status
        vendor.setStatus(VendorStatus.PENDING);

        // 6. Save the vendor
        Vendor saved = vendorRepository.save(vendor);

        // 7. Convert entity to response DTO
        return VendorResponse.from(saved);
    }

    @Transactional
    public VendorResponse approve(UUID id) {
        // 1. load vendor by id (findById + orElseThrow, same pattern as Step 1)
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + id));
        // 2. GUARD: only a PENDING (or SUSPENDED, your call) vendor may be approved
        //    -> if not, throw for now (Step 3 makes it a clean 4xx)
        if (vendor.getStatus() != VendorStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING vendors can be approved"
            );
        }
        // 3. vendor.setStatus(APPROVED)
        vendor.setStatus(VendorStatus.APPROVED);
        // 4. save, return VendorResponse.from(saved)
        Vendor saved = vendorRepository.save(vendor);
        return VendorResponse.from(saved);


    }

    @Transactional
    public VendorResponse suspend(UUID vendorId){
        // 1. load vendor by id (findById + orElseThrow, same pattern as Step 1)
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + vendorId));
        // 2. GUARD: only a PENDING (or SUSPENDED, your call) vendor may be approved
        //    -> if not, throw for now (Step 3 makes it a clean 4xx)
        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED vendors can be suspended"
            );
        }
        // 3. vendor.setStatus(APPROVED)
        vendor.setStatus(VendorStatus.SUSPENDED);
        // 4. save, return VendorResponse.from(saved)
        Vendor saved = vendorRepository.save(vendor);
        return VendorResponse.from(saved);

    }

    @Transactional
    public Page<VendorResponse> listApproved(Pageable pageable) {
        return vendorRepository.findByStatus(VendorStatus.APPROVED,pageable).map(VendorResponse::from);
    }


}
