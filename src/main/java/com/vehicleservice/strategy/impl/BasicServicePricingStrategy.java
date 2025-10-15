package com.vehicleservice.strategy.impl;

import com.vehicleservice.strategy.PricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Strategy pattern implementation for basic service pricing
@Component
@org.springframework.context.annotation.Scope("singleton")
public class BasicServicePricingStrategy implements PricingStrategy {

    // Base prices for basic services (in Sri Lankan Rupees - LKR)
    private static final BigDecimal OIL_CHANGE_PRICE = new BigDecimal("3600.00");
    private static final BigDecimal TIRE_ROTATION_PRICE = new BigDecimal("2000.00");
    private static final BigDecimal AIR_FILTER_REPLACEMENT_PRICE = new BigDecimal("2500.00");
    private static final BigDecimal SPARK_PLUG_REPLACEMENT_PRICE = new BigDecimal("3000.00");
    private static final BigDecimal DEFAULT_BASIC_PRICE = new BigDecimal("2000.00");

    // No additional charges for basic services

    @Override
    public BigDecimal calculateBasePrice(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return DEFAULT_BASIC_PRICE;
        }

        switch (serviceType.toLowerCase().trim()) {
            case "oil change":
            case "basic oil change":
                return OIL_CHANGE_PRICE;
            case "tire service":
            case "tire rotation":
                return TIRE_ROTATION_PRICE;
            case "general maintenance":
            case "basic maintenance":
                return DEFAULT_BASIC_PRICE;
            case "air filter replacement":
            case "air filter":
                return AIR_FILTER_REPLACEMENT_PRICE;
            case "spark plug replacement":
            case "spark plugs":
                return SPARK_PLUG_REPLACEMENT_PRICE;
            default:
                return DEFAULT_BASIC_PRICE;
        }
    }

    @Override
    public BigDecimal calculateAdditionalCharges(String serviceType, BigDecimal basePrice) {
        // No additional charges for basic services
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateTotalPrice(String serviceType, BigDecimal basePrice, BigDecimal additionalCharges) {
        return basePrice.add(additionalCharges);
    }

    @Override
    public String getServiceCategory() {
        return "BASIC_SERVICE";
    }

    @Override
    public boolean appliesTo(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return false;
        }

        String lowerServiceType = serviceType.toLowerCase().trim();
        return lowerServiceType.contains("oil change") ||
                lowerServiceType.contains("tire service") ||
                lowerServiceType.contains("general maintenance") ||
                lowerServiceType.contains("air filter") ||
                lowerServiceType.contains("spark plug") ||
                lowerServiceType.contains("basic service");
    }
}
