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

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ApplicationEventPublisher events;

    private static final Set<OrderStatus> VENDOR_ALLOWED = EnumSet.of(
            OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.PREPARING,
            OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

    public OrderService(OrderRepository orderRepository, MenuItemRepository menuItemRepository, VendorRepository vendorRepository, UserRepository userRepository, PaymentService paymentService, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.events = events;
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

        events.publishEvent(new OrderStatusChanged(
                saved.getId(),
                saved.getCustomer().getId(),
                saved.getVendor().getId(),
                null,                    // brand-new order — no previous status
                OrderStatus.PLACED,
                Instant.now()));

        return OrderResponse.from(saved);


    }


    @Transactional
    public OrderResponse advanceStatus(String userId, UUID orderId, OrderStatus target) {
        // GATE 1 (role) already enforced by @PreAuthorize on the controller.

        // Load the order (404 if it doesn't exist).
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // GATE 2 — OWNERSHIP: this vendor must own the order's vendor.
        // Walk order -> vendor -> owner -> id, compare to the caller.
        if (!order.getVendor().getOwner().getId().equals(UUID.fromString(userId))) {
            throw new ForbiddenException("You do not own this order's vendor");
        }

        // GATE 3 — ACTOR PERMISSION: is this a status a vendor is allowed to set?
        if (!VENDOR_ALLOWED.contains(target)) {
            throw new IllegalStateException("A vendor cannot move an order to " + target);
        }

        // GATE 4 — STATE-MACHINE LEGALITY (+ save) happens inside the helper.
        return applyTransition(order, target);
    }


    @Transactional
    public OrderResponse cancelOrder(String userId, UUID orderId) {
        // Load the order (404 if missing).
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // OWNERSHIP: the caller must be the customer who placed this order.
        if (!order.getCustomer().getId().equals(UUID.fromString(userId))) {
            throw new ForbiddenException("You can only cancel your own order");
        }

        // Target is fixed to CANCELLED; the state machine decides if it's legal now.
        return applyTransition(order, OrderStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> myOrders(String userId, Pageable pageable) {
        return orderRepository.findByCustomerId(UUID.fromString(userId), pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> vendorOrders(String userId, Pageable pageable) {
        return orderRepository.findByVendorOwnerId(UUID.fromString(userId), pageable)
                .map(OrderResponse::from);
    }


    private OrderResponse applyTransition(Order order, OrderStatus target) {
        OrderStatus previous = order.getStatus();          // Bug 2: capture BEFORE changing
        if (!previous.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Illegal transition: " + previous + " -> " + target);
        }
        order.setStatus(target);
        Order saved = orderRepository.save(order);          // Bug 3: single save

        events.publishEvent(new OrderStatusChanged(
                saved.getId(),
                saved.getCustomer().getId(),
                saved.getVendor().getId(),
                previous,                                    // Bug 1: real old status
                target,                                      // Bug 1: real new status
                Instant.now()));

        return OrderResponse.from(saved);
    }








}
