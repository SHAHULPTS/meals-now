package com.mealsnow.catalog;

import com.mealsnow.catalog.dto.CreateMenuItemRequest;
import com.mealsnow.catalog.dto.MenuItemResponse;
import com.mealsnow.catalog.dto.UpdateMenuItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vendors/{vendorId}/menu-items")
@PreAuthorize("hasRole('VENDOR')")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> create(
            @PathVariable UUID vendorId,
            @Valid @RequestBody CreateMenuItemRequest req,
            @AuthenticationPrincipal String userId) {
        MenuItemResponse res = menuItemService.create(vendorId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> list(
            @PathVariable UUID vendorId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(menuItemService.list(vendorId, userId));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<MenuItemResponse> update(
            @PathVariable UUID vendorId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest req,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(menuItemService.update(vendorId, itemId, userId, req));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID vendorId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal String userId) {
        menuItemService.delete(vendorId, itemId, userId);
        return ResponseEntity.noContent().build();      // 204
    }

    @PatchMapping("/{itemId}/availability")
    public ResponseEntity<MenuItemResponse> setAvailability(
            @PathVariable UUID vendorId,
            @PathVariable UUID itemId,
            @RequestParam boolean available,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(
                menuItemService.setAvailability(vendorId, itemId, userId, available));
    }
}