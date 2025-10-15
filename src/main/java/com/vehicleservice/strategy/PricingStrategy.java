package com.vehicleservice.strategy;

// Import statements for pricing calculations
import java.math.BigDecimal;

// Strategy pattern interface for pricing calculations
public interface PricingStrategy {

    // Calculate base price for a service
    BigDecimal calculateBasePrice(String serviceType);

    // Calculate additional charges for service complexity
    BigDecimal calculateAdditionalCharges(String serviceType, BigDecimal basePrice);

    // Calculate total price including all charges
    BigDecimal calculateTotalPrice(String serviceType, BigDecimal basePrice, BigDecimal additionalCharges);

    // Get service category for this strategy
    String getServiceCategory();

    // Check if strategy applies to service type
    boolean appliesTo(String serviceType);
}
