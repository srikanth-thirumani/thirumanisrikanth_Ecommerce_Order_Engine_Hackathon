package com.ecommerce.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResult {

    private boolean success;
    private String transactionId;
    private String paymentMethod;
    private BigDecimal amountCharged;
    private String failureReason;
    private LocalDateTime processedAt;

    public PaymentResult() {}

    public static PaymentResult success(String transactionId, String method, BigDecimal amount) {
        PaymentResult res = new PaymentResult();
        res.setSuccess(true);
        res.setTransactionId(transactionId);
        res.setPaymentMethod(method);
        res.setAmountCharged(amount);
        res.setProcessedAt(LocalDateTime.now());
        return res;
    }

    public static PaymentResult failure(String method, String reason) {
        PaymentResult res = new PaymentResult();
        res.setSuccess(false);
        res.setPaymentMethod(method);
        res.setFailureReason(reason);
        res.setProcessedAt(LocalDateTime.now());
        return res;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmountCharged() { return amountCharged; }
    public void setAmountCharged(BigDecimal amountCharged) { this.amountCharged = amountCharged; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public static PaymentResultBuilder builder() {
        return new PaymentResultBuilder();
    }

    public static class PaymentResultBuilder {
        private boolean success;
        private String transactionId;
        private String paymentMethod;
        private BigDecimal amountCharged;
        private String failureReason;
        private LocalDateTime processedAt;

        public PaymentResultBuilder success(boolean success) { this.success = success; return this; }
        public PaymentResultBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public PaymentResultBuilder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public PaymentResultBuilder amountCharged(BigDecimal amountCharged) { this.amountCharged = amountCharged; return this; }
        public PaymentResultBuilder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public PaymentResultBuilder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }

        public PaymentResult build() {
            PaymentResult r = new PaymentResult();
            r.setSuccess(success);
            r.setTransactionId(transactionId);
            r.setPaymentMethod(paymentMethod);
            r.setAmountCharged(amountCharged);
            r.setFailureReason(failureReason);
            r.setProcessedAt(processedAt);
            return r;
        }
    }
}
