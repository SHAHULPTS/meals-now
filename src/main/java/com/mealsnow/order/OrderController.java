package com.mealsnow.order;

import com.mealsnow.order.dto.OrderResponse;
import com.mealsnow.order.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mealsnow.order.dto.AdvanceStatusRequest;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse place(@AuthenticationPrincipal String userId,
                               @Valid @RequestBody PlaceOrderRequest req) {
        return orderService.placeOrder(userId, req);
    }

    @PostMapping("/{orderId}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public OrderResponse advance(@AuthenticationPrincipal String userId,
                                 @PathVariable UUID orderId,
                                 @Valid @RequestBody AdvanceStatusRequest req) {
        return orderService.advanceStatus(userId, orderId, req.target());
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse cancel(@AuthenticationPrincipal String userId,
                                @PathVariable UUID orderId) {
        return orderService.cancelOrder(userId, orderId);
    }
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public Page<OrderResponse> myOrders(@AuthenticationPrincipal String userId, Pageable pageable) {
        return orderService.myOrders(userId, pageable);
    }

    @GetMapping("/vendor")
    @PreAuthorize("hasRole('VENDOR')")
    public Page<OrderResponse> vendorOrders(@AuthenticationPrincipal String userId, Pageable pageable) {
        return orderService.vendorOrders(userId, pageable);
    }



}
