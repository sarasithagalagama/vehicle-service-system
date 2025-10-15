package com.vehicleservice.strategy.impl;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.strategy.PaymentProcessingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// Strategy pattern implementation for cash payment processing
@Component
@org.springframework.context.annotation.Scope("singleton")
public class CashPaymentStrategy implements PaymentProcessingStrategy {

    private static final BigDecimal CASH_PROCESSING_FEE_RATE = BigDecimal.ZERO; // No fees for cash
    private static final BigDecimal MINIMUM_CASH_AMOUNT = new BigDecimal("100.00"); // Minimum 100 LKR
    private static final BigDecimal MAXIMUM_CASH_AMOUNT = new BigDecimal("100000.00"); // Maximum 100,000 LKR

    @Override
    public PaymentResult processPayment(Booking booking, BigDecimal amount, String paymentMethod) {
        // Validate payment first
        PaymentValidationResult validation = validatePayment(booking, amount, paymentMethod);
        if (!validation.isValid()) {
            return new PaymentResult(false, null, validation.getErrorMessage(),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Process cash payment (immediate)
        String transactionId = "CASH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal processingFees = calculateProcessingFees(amount, paymentMethod);

        // Update booking payment status
        updateBookingPaymentStatus(booking, amount);

        return new PaymentResult(true, transactionId,
                "Cash payment processed successfully",
                amount, processingFees);
    }

    @Override
    public BigDecimal calculateProcessingFees(BigDecimal amount, String paymentMethod) {
        return amount.multiply(CASH_PROCESSING_FEE_RATE);
    }

    @Override
    public PaymentValidationResult validatePayment(Booking booking, BigDecimal amount, String paymentMethod) {
        // Check if amount is within valid range
        if (amount.compareTo(MINIMUM_CASH_AMOUNT) < 0) {
            return new PaymentValidationResult(false,
                    "Cash payment amount must be at least " + MINIMUM_CASH_AMOUNT + " LKR");
        }

        if (amount.compareTo(MAXIMUM_CASH_AMOUNT) > 0) {
            return new PaymentValidationResult(false,
                    "Cash payment amount cannot exceed " + MAXIMUM_CASH_AMOUNT + " LKR");
        }

        // Check if booking exists and is valid
        if (booking == null) {
            return new PaymentValidationResult(false, "Invalid booking");
        }

        // Check if payment amount doesn't exceed total price
        if (amount.compareTo(booking.getTotalPrice()) > 0) {
            return new PaymentValidationResult(false,
                    "Payment amount cannot exceed total booking price");
        }

        return new PaymentValidationResult(true, null);
    }

    @Override
    public String getPaymentMethod() {
        return "CASH";
    }

    @Override
    public boolean appliesTo(String paymentMethod) {
        return paymentMethod != null &&
                paymentMethod.toUpperCase().trim().equals("CASH");
    }

    /**
     * Update booking payment status after successful cash payment
     */
    private void updateBookingPaymentStatus(Booking booking, BigDecimal amount) {
        BigDecimal currentPaidAmount = booking.getPaidAmount() != null ? booking.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaidAmount.add(amount);

        booking.setPaidAmount(newPaidAmount);
        booking.setRemainingAmount(booking.getTotalPrice().subtract(newPaidAmount));

        // Update payment status based on remaining amount
        if (booking.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            booking.setPaymentStatus(Booking.PaymentStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            booking.setPaymentStatus(Booking.PaymentStatus.PARTIAL);
        } else {
            booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
        }
    }
}
