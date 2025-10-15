package com.vehicleservice.strategy;

import com.vehicleservice.entity.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Payment Processing Strategy Manager - Manages payment processing strategies
@Component
@org.springframework.context.annotation.Scope("singleton")
public class PaymentProcessingStrategyManager {

    @Autowired
    private List<PaymentProcessingStrategy> paymentStrategies;

    // Process a payment for a booking using the appropriate strategy
    public PaymentProcessingStrategy.PaymentResult processPayment(Booking booking, BigDecimal amount,
            String paymentMethod) {
        PaymentProcessingStrategy strategy = selectStrategy(paymentMethod);

        if (strategy == null) {
            return new PaymentProcessingStrategy.PaymentResult(false, null,
                    "Unsupported payment method: " + paymentMethod,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return strategy.processPayment(booking, amount, paymentMethod);
    }

    // Calculate processing fees for a payment using the appropriate strategy
    public BigDecimal calculateProcessingFees(BigDecimal amount, String paymentMethod) {
        PaymentProcessingStrategy strategy = selectStrategy(paymentMethod);

        if (strategy == null) {
            return BigDecimal.ZERO;
        }

        return strategy.calculateProcessingFees(amount, paymentMethod);
    }

    /**
     * Validate a payment before processing using the appropriate strategy
     * 
     * @param booking       The booking
     * @param amount        The amount
     * @param paymentMethod The payment method
     * @return Validation result
     */
    public PaymentProcessingStrategy.PaymentValidationResult validatePayment(Booking booking, BigDecimal amount,
            String paymentMethod) {
        PaymentProcessingStrategy strategy = selectStrategy(paymentMethod);

        if (strategy == null) {
            return new PaymentProcessingStrategy.PaymentValidationResult(false,
                    "Unsupported payment method: " + paymentMethod);
        }

        return strategy.validatePayment(booking, amount, paymentMethod);
    }

    /**
     * Get payment processing information for a payment method
     * 
     * @param paymentMethod The payment method
     * @return Payment processing information
     */
    public PaymentProcessingInfo getPaymentProcessingInfo(String paymentMethod) {
        PaymentProcessingStrategy strategy = selectStrategy(paymentMethod);

        if (strategy == null) {
            return new PaymentProcessingInfo(paymentMethod, "UNSUPPORTED",
                    BigDecimal.ZERO, "Payment method not supported");
        }

        // Calculate sample fees for information
        BigDecimal sampleAmount = new BigDecimal("1000.00");
        BigDecimal processingFees = strategy.calculateProcessingFees(sampleAmount, paymentMethod);

        return new PaymentProcessingInfo(
                paymentMethod,
                strategy.getPaymentMethod(),
                processingFees,
                "Processing fees: " + processingFees + " LKR per 1000 LKR transaction");
    }

    /**
     * Select the appropriate payment processing strategy for the given payment
     * method
     * 
     * @param paymentMethod The payment method
     * @return The appropriate payment processing strategy
     */
    private PaymentProcessingStrategy selectStrategy(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return null;
        }

        // Find the first strategy that applies to this payment method
        for (PaymentProcessingStrategy strategy : paymentStrategies) {
            if (strategy.appliesTo(paymentMethod)) {
                return strategy;
            }
        }

        return null;
    }

    /**
     * Get all available payment processing strategies
     * 
     * @return List of all payment processing strategies
     */
    public List<PaymentProcessingStrategy> getAllStrategies() {
        return paymentStrategies;
    }

    /**
     * Get the strategy that applies to a specific payment method
     * 
     * @param paymentMethod The payment method
     * @return The applicable strategy or null if none found
     */
    public PaymentProcessingStrategy getStrategyForPaymentMethod(String paymentMethod) {
        return selectStrategy(paymentMethod);
    }

    /**
     * Get all supported payment methods
     * 
     * @return List of supported payment methods
     */
    public List<String> getSupportedPaymentMethods() {
        return paymentStrategies.stream()
                .map(PaymentProcessingStrategy::getPaymentMethod)
                .distinct()
                .toList();
    }

    /**
     * Payment Processing Info class to hold payment processing information
     */
    public static class PaymentProcessingInfo {
        private final String requestedMethod;
        private final String supportedMethod;
        private final BigDecimal sampleProcessingFees;
        private final String description;

        public PaymentProcessingInfo(String requestedMethod, String supportedMethod,
                BigDecimal sampleProcessingFees, String description) {
            this.requestedMethod = requestedMethod;
            this.supportedMethod = supportedMethod;
            this.sampleProcessingFees = sampleProcessingFees;
            this.description = description;
        }

        // Getters
        public String getRequestedMethod() {
            return requestedMethod;
        }

        public String getSupportedMethod() {
            return supportedMethod;
        }

        public BigDecimal getSampleProcessingFees() {
            return sampleProcessingFees;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("PaymentProcessingInfo{requested='%s', supported='%s', fees=%s, description='%s'}",
                    requestedMethod, supportedMethod, sampleProcessingFees, description);
        }
    }
}
