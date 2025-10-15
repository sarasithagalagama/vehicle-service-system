package com.vehicleservice.strategy;

import com.vehicleservice.entity.Booking;
import java.math.BigDecimal;

// Strategy pattern interface for payment processing
public interface PaymentProcessingStrategy {

    // Process a payment for a booking
    PaymentResult processPayment(Booking booking, BigDecimal amount, String paymentMethod);

    // Calculate processing fees for the payment
    BigDecimal calculateProcessingFees(BigDecimal amount, String paymentMethod);

    // Validate payment before processing
    PaymentValidationResult validatePayment(Booking booking, BigDecimal amount, String paymentMethod);

    // Get the payment method this strategy handles
    String getPaymentMethod();

    // Check if this strategy applies to the given payment method
    boolean appliesTo(String paymentMethod);

    // Payment Result class
    class PaymentResult {
        private final boolean success;
        private final String transactionId;
        private final String message;
        private final BigDecimal processedAmount;
        private final BigDecimal processingFees;

        public PaymentResult(boolean success, String transactionId, String message,
                BigDecimal processedAmount, BigDecimal processingFees) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
            this.processedAmount = processedAmount;
            this.processingFees = processingFees;
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getMessage() {
            return message;
        }

        public BigDecimal getProcessedAmount() {
            return processedAmount;
        }

        public BigDecimal getProcessingFees() {
            return processingFees;
        }
    }

    // Payment Validation Result class
    class PaymentValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public PaymentValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        // Getters
        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
