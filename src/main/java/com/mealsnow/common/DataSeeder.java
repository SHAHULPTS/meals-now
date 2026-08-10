package com.mealsnow.common;

import com.mealsnow.identity.*;
import com.mealsnow.vendor.*;
import com.mealsnow.catalog.MenuItem;
import com.mealsnow.catalog.MenuItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final MenuItemRepository menuItemRepository;

    public DataSeeder(UserRepository userRepository,
                      VendorRepository vendorRepository,
                      MenuItemRepository menuItemRepository) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded — stay idempotent
        }

        // 1. a vendor-owner user
        User owner = new User();
        owner.setEmail("owner@meals.com");
        owner.setPasswordHash("x".repeat(60));
        owner.setRole(Role.VENDOR);
        userRepository.save(owner);

        // 2. a customer
        User customer = new User();
        customer.setEmail("customer@meals.com");
        customer.setPasswordHash("x".repeat(60));
        customer.setRole(Role.CUSTOMER);
        userRepository.save(customer);

        // 3. a vendor owned by that owner
        Vendor vendor = new Vendor();
        vendor.setOwner(owner);
        vendor.setName("Sample Kitchen");
        vendor.setAddress("123 Test St");
        vendor.setStatus(VendorStatus.APPROVED);
        vendorRepository.save(vendor);

        // 4. two menu items for that vendor
        MenuItem pizza = new MenuItem();
        pizza.setVendor(vendor);
        pizza.setName("Margherita Pizza");
        pizza.setPrice(new BigDecimal("9.99"));
        pizza.setAvailable(true);
        menuItemRepository.save(pizza);

        MenuItem cola = new MenuItem();
        cola.setVendor(vendor);
        cola.setName("Cola");
        cola.setPrice(new BigDecimal("1.50"));
        cola.setAvailable(true);
        menuItemRepository.save(cola);
    }
}