package com.mealsnow.order;


import com.mealsnow.catalog.MenuItem;
import com.mealsnow.catalog.MenuItemRepository;
import com.mealsnow.common.error.NotFoundException;
import com.mealsnow.common.error.ForbiddenException;   // if you use it for the vendor-mismatch guard
import com.mealsnow.identity.User;
import com.mealsnow.identity.UserRepository;
import com.mealsnow.order.dto.OrderLine;
import com.mealsnow.order.dto.OrderResponse;
import com.mealsnow.order.dto.PlaceOrderRequest;
import com.mealsnow.order.payment.PaymentResult;
import com.mealsnow.order.payment.PaymentService;
import com.mealsnow.vendor.Vendor;
import com.mealsnow.vendor.VendorRepository;
import com.mealsnow.vendor.VendorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, MenuItemRepository menuItemRepository, VendorRepository vendorRepository, UserRepository userRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }


    @Transactional
    public OrderResponse placeOrder(String userId, PlaceOrderRequest req){
        // 1. Load customer: userRepository.findById(UUID.fromString(userId))
        //    -> orElseThrow(() -> new NotFoundException("User not found: " + userId))
        User customer = userRepository.findById(UUID.fromString(userId)).
                orElseThrow(() -> new NotFoundException("User not found: " + userId));


        // 2. Load vendor by req.vendorId() -> NotFound if missing.
        //    GUARD: if status != APPROVED -> throw NotFoundException (don't order from a non-approved vendor)
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + req.vendorId()));

        // 3. Build the order shell:
            Order order = new Order();
            order.setCustomer(customer);
            order.setVendor(vendor);
            order.setStatus(OrderStatus.PLACED);   // Step 2 will move this behind payment
            BigDecimal total = BigDecimal.ZERO;


        for (OrderLine line : req.items()) {
            MenuItem item = menuItemRepository.findById(line.menuItemId())
                    .orElseThrow(() -> new NotFoundException("MenuItem not found: " + line.menuItemId()));

            if (!item.getVendor().getId().equals(req.vendorId())) {
                throw new NotFoundException("MenuItem not on this vendor's menu: " + line.menuItemId());
            }
            if (!item.isAvailable()) {
                throw new IllegalStateException("Item not available: " + item.getName());
            }

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(item);
            oi.setItemName(item.getName());
            oi.setUnitPrice(item.getPrice());
            oi.setQuantity(line.quantity());
            order.getItems().add(oi);
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        order.setTotal(total);
        PaymentResult payment = paymentService.charge(total);
        if (!payment.success()) {
            throw new IllegalStateException("Payment failed: " + payment.failureReason());
        }

        Order saved = orderRepository.save(order);   // reached only if payment succeeded
        return OrderResponse.from(saved);


    }
}
