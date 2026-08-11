package com.mealsnow.catalog;

import com.mealsnow.catalog.dto.CreateMenuItemRequest;
import com.mealsnow.catalog.dto.MenuItemResponse;
import com.mealsnow.catalog.dto.UpdateMenuItemRequest;
import com.mealsnow.common.error.ForbiddenException;
import com.mealsnow.common.error.NotFoundException;
import com.mealsnow.vendor.Vendor;
import com.mealsnow.vendor.VendorRepository;
import com.mealsnow.vendor.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final VendorRepository vendorRepository;

    public MenuItemService(MenuItemRepository menuItemRepository,
                           VendorRepository vendorRepository) {
        this.menuItemRepository = menuItemRepository;
        this.vendorRepository = vendorRepository;
    }

    // ---------- public operations ----------

    @Transactional
    public MenuItemResponse create(UUID vendorId, String userId, CreateMenuItemRequest req) {
        Vendor vendor = loadOwnedVendor(vendorId, userId);

        MenuItem item = new MenuItem();
        item.setVendor(vendor);
        item.setName(req.name());
        item.setPrice(req.price());
        item.setDescription(req.description());
        item.setCategory(req.category());
        item.setAvailable(true);                 // server default

        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> list(UUID vendorId, String userId) {
        loadOwnedVendor(vendorId, userId);       // ownership gate; return value unused
        return menuItemRepository.findByVendorId(vendorId)
                .stream().map(MenuItemResponse::from).toList();
    }

    @Transactional
    public MenuItemResponse update(UUID vendorId, UUID itemId, String userId,
                                   UpdateMenuItemRequest req) {
        loadOwnedVendor(vendorId, userId);
        MenuItem item = loadItemOfVendor(vendorId, itemId);

        item.setName(req.name());
        item.setPrice(req.price());
        item.setDescription(req.description());
        item.setCategory(req.category());

        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @Transactional
    public void delete(UUID vendorId, UUID itemId, String userId) {
        loadOwnedVendor(vendorId, userId);
        MenuItem item = loadItemOfVendor(vendorId, itemId);
        menuItemRepository.delete(item);
    }

    @Transactional
    public MenuItemResponse setAvailability(UUID vendorId, UUID itemId, String userId,
                                            boolean available) {
        loadOwnedVendor(vendorId, userId);
        MenuItem item = loadItemOfVendor(vendorId, itemId);
        item.setAvailable(available);
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public Page<MenuItemResponse> publicMenu(UUID vendorId, Pageable pageable) {
        // 1. load the vendor by id -> NotFoundException if missing
        // 2. GUARD: if vendor.getStatus() != VendorStatus.APPROVED -> throw NotFoundException
        //    (a customer must not see a pending/suspended vendor's menu)
        // 3. return menuItemRepository
        //         .findByVendorIdAndAvailableTrue(vendorId, pageable)
        //         .map(MenuItemResponse::from);

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + vendorId));

        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new NotFoundException("Vendor not found: " + vendorId);
        }

        return menuItemRepository.findByVendorIdAndAvailableTrue(vendorId, pageable)
                .map(MenuItemResponse::from);
    }


    // ---------- private helpers ----------

    private Vendor loadOwnedVendor(UUID vendorId, String userId) {
        Vendor v = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + vendorId));
        if (!v.getOwner().getId().equals(UUID.fromString(userId))) {
            throw new ForbiddenException("You do not own this vendor");
        }
        return v;
    }


    private MenuItem loadItemOfVendor(UUID vendorId, UUID itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Menu item not found: " + itemId));
        if (!item.getVendor().getId().equals(vendorId)) {
            // item exists but isn't part of THIS vendor's menu -> treat as not found
            throw new NotFoundException("Menu item not found for this vendor: " + itemId);
        }
        return item;
    }
}