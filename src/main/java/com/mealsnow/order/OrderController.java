package com.mealsnow.order;

import com.mealsnow.order.dto.OrderResponse;
import com.mealsnow.order.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
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
}
