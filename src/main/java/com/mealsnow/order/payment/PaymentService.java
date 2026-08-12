package com.mealsnow.order.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    PaymentResult charge( BigDecimal amount);
}