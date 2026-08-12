package com.mealsnow.order.payment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class MockPaymentService implements PaymentService {

    @Override
    public PaymentResult charge(BigDecimal amount) {
        // Always succeeds for now. To test the rollback path later,
        // return PaymentResult.fail("insufficient funds");
        return PaymentResult.ok("MOCK-" + UUID.randomUUID());
    }
}

