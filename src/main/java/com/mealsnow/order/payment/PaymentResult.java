package com.mealsnow.order.payment;

public record PaymentResult(boolean success, String transactionId, String failureReason) {
    public static PaymentResult ok(String txId) { return new PaymentResult(true, txId, null); }
    public static PaymentResult fail(String reason) { return new PaymentResult(false, null, reason); }
}