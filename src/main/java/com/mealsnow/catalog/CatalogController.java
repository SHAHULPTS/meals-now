package com.mealsnow.catalog;

import com.mealsnow.catalog.dto.MenuItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/vendors/{vendorId}/menu")
public class CatalogController {

    private final MenuItemService menuItemService;

    public CatalogController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping
    public ResponseEntity<Page<MenuItemResponse>> publicMenu(
            @PathVariable UUID vendorId,
            Pageable pageable) {
        return ResponseEntity.ok(menuItemService.publicMenu(vendorId, pageable));
    }
}