package com.vehicleservice.strategy.impl;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.strategy.PaymentProcessingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// Strategy pattern implementation for card payment processing
@Component
@org.springframework.context.annotation.Scope("singleton")
public class CardPaymentStrategy implements PaymentProcessingStrategy {

    // Processing fees for different card types
    private static final BigDecimal VISA_PROCESSING_FEE_RATE = new BigDecimal("0.025"); // 2.5% for Visa
    private static final BigDecimal MASTERCARD_PROCESSING_FEE_RATE = new BigDecimal("0.03"); // 3.0% for Mastercard
    private static final BigDecimal AMEX_PROCESSING_FEE_RATE = new BigDecimal("0.035"); // 3.5% for American Express
    private static final BigDecimal CREDIT_CARD_PROCESSING_FEE_RATE = new BigDecimal("0.028"); // 2.8% for generic
                                                                                               // credit cards
    private static final BigDecimal DEBIT_CARD_PROCESSING_FEE_RATE = new BigDecimal("0.02"); // 2.0% for debit cards

    // Minimum and maximum amounts
    private static final BigDecimal MINIMUM_CARD_AMOUNT = new BigDecimal("500.00"); // Minimum 500 LKR
    private static final BigDecimal MAXIMUM_CARD_AMOUNT = new BigDecimal("500000.00"); // Maximum 500,000 LKR

    // Minimum processing fees for different card types
    private static final BigDecimal VISA_MINIMUM_FEE = new BigDecimal("25.00"); // Minimum 25 LKR for Visa
    private static final BigDecimal MASTERCARD_MINIMUM_FEE = new BigDecimal("30.00"); // Minimum 30 LKR for Mastercard
    private static final BigDecimal AMEX_MINIMUM_FEE = new BigDecimal("35.00"); // Minimum 35 LKR for Amex
    private static final BigDecimal CREDIT_CARD_MINIMUM_FEE = new BigDecimal("28.00"); // Minimum 28 LKR for credit
                                                                                       // cards
    private static final BigDecimal DEBIT_CARD_MINIMUM_FEE = new BigDecimal("20.00"); // Minimum 20 LKR for debit cards

    @Override
    public PaymentResult processPayment(Booking booking, BigDecimal amount, String paymentMethod) {
        // Validate payment first
        PaymentValidationResult validation = validatePayment(booking, amount, paymentMethod);
        if (!validation.isValid()) {
            return new PaymentResult(false, null, validation.getErrorMessage(),
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Simulate card payment processing (in real implementation, integrate with
        // payment gateway)
        boolean paymentSuccessful = simulateCardPaymentProcessing(booking, amount);

        if (!paymentSuccessful) {
            return new PaymentResult(false, null,
                    "Card payment processing failed. Please try again.",
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Process successful card payment
        String transactionId = "CARD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal processingFees = calculateProcessingFees(amount, paymentMethod);

        // Update booking payment status
        updateBookingPaymentStatus(booking, amount);

        return new PaymentResult(true, transactionId,
                "Card payment processed successfully",
                amount, processingFees);
    }

    @Override
    public BigDecimal calculateProcessingFees(BigDecimal amount, String paymentMethod) {
        if (paymentMethod == null) {
            return BigDecimal.ZERO;
        }

        String upperPaymentMethod = paymentMethod.toUpperCase().trim();
        BigDecimal feeRate;
        BigDecimal minimumFee;

        // Determine fee rate and minimum fee based on card type
        switch (upperPaymentMethod) {
            case "VISA":
                feeRate = VISA_PROCESSING_FEE_RATE;
                minimumFee = VISA_MINIMUM_FEE;
                break;
            case "MASTERCARD":
                feeRate = MASTERCARD_PROCESSING_FEE_RATE;
                minimumFee = MASTERCARD_MINIMUM_FEE;
                break;
            case "AMEX":
            case "AMERICAN_EXPRESS":
                feeRate = AMEX_PROCESSING_FEE_RATE;
                minimumFee = AMEX_MINIMUM_FEE;
                break;
            case "CREDIT_CARD":
                feeRate = CREDIT_CARD_PROCESSING_FEE_RATE;
                minimumFee = CREDIT_CARD_MINIMUM_FEE;
                break;
            case "DEBIT_CARD":
                feeRate = DEBIT_CARD_PROCESSING_FEE_RATE;
                minimumFee = DEBIT_CARD_MINIMUM_FEE;
                break;
            default:
                // Default to Visa rates for unknown card types
                feeRate = VISA_PROCESSING_FEE_RATE;
                minimumFee = VISA_MINIMUM_FEE;
                break;
        }

        BigDecimal calculatedFee = amount.multiply(feeRate);
        return calculatedFee.compareTo(minimumFee) < 0 ? minimumFee : calculatedFee;
    }

    @Override
    public PaymentValidationResult validatePayment(Booking booking, BigDecimal amount, String paymentMethod) {
        // Check if amount is within valid range
        if (amount.compareTo(MINIMUM_CARD_AMOUNT) < 0) {
            return new PaymentValidationResult(false,
                    "Card payment amount must be at least " + MINIMUM_CARD_AMOUNT + " LKR");
        }

        if (amount.compareTo(MAXIMUM_CARD_AMOUNT) > 0) {
            return new PaymentValidationResult(false,
                    "Card payment amount cannot exceed " + MAXIMUM_CARD_AMOUNT + " LKR");
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

        // Validate card payment method
        if (!isValidCardPaymentMethod(paymentMethod)) {
            return new PaymentValidationResult(false,
                    "Invalid card payment method. Supported methods: VISA, MASTERCARD, AMEX");
        }

        return new PaymentValidationResult(true, null);
    }

    @Override
    public String getPaymentMethod() {
        return "CARD";
    }

    @Override
    public boolean appliesTo(String paymentMethod) {
        if (paymentMethod == null) {
            return false;
        }

        String upperPaymentMethod = paymentMethod.toUpperCase().trim();
        return upperPaymentMethod.equals("CARD") ||
                upperPaymentMethod.equals("VISA") ||
                upperPaymentMethod.equals("MASTERCARD") ||
                upperPaymentMethod.equals("AMEX") ||
                upperPaymentMethod.equals("CREDIT_CARD") ||
                upperPaymentMethod.equals("DEBIT_CARD");
    }

    /**
     * Simulate card payment processing (in real implementation, integrate with
     * payment gateway)
     */
    private boolean simulateCardPaymentProcessing(Booking booking, BigDecimal amount) {
        // Simulate processing delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 95% success rate
        return Math.random() > 0.05;
    }

    /**
     * Validate card payment method
     */
    private boolean isValidCardPaymentMethod(String paymentMethod) {
        if (paymentMethod == null) {
            return false;
        }

        String upperMethod = paymentMethod.toUpperCase().trim();
        return upperMethod.equals("VISA") ||
                upperMethod.equals("MASTERCARD") ||
                upperMethod.equals("AMEX") ||
                upperMethod.equals("CREDIT_CARD") ||
                upperMethod.equals("DEBIT_CARD");
    }

    /**
     * Get fee information for a specific card type
     */
    public String getFeeInformation(String paymentMethod) {
        if (paymentMethod == null) {
            return "Unknown card type";
        }

        String upperPaymentMethod = paymentMethod.toUpperCase().trim();
        BigDecimal feeRate;
        BigDecimal minimumFee;

        switch (upperPaymentMethod) {
            case "VISA":
                feeRate = VISA_PROCESSING_FEE_RATE;
                minimumFee = VISA_MINIMUM_FEE;
                break;
            case "MASTERCARD":
                feeRate = MASTERCARD_PROCESSING_FEE_RATE;
                minimumFee = MASTERCARD_MINIMUM_FEE;
                break;
            case "AMEX":
            case "AMERICAN_EXPRESS":
                feeRate = AMEX_PROCESSING_FEE_RATE;
                minimumFee = AMEX_MINIMUM_FEE;
                break;
            case "CREDIT_CARD":
                feeRate = CREDIT_CARD_PROCESSING_FEE_RATE;
                minimumFee = CREDIT_CARD_MINIMUM_FEE;
                break;
            case "DEBIT_CARD":
                feeRate = DEBIT_CARD_PROCESSING_FEE_RATE;
                minimumFee = DEBIT_CARD_MINIMUM_FEE;
                break;
            default:
                feeRate = VISA_PROCESSING_FEE_RATE;
                minimumFee = VISA_MINIMUM_FEE;
                break;
        }

        return String.format("%s: %.1f%% fee (minimum %s LKR)",
                upperPaymentMethod,
                feeRate.multiply(new BigDecimal("100")),
                minimumFee);
    }

    /**
     * Update booking payment status after successful card payment
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
