package com.vehicleservice.strategy.impl;

import com.vehicleservice.strategy.PricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Strategy pattern implementation for advanced service pricing
@Component
@org.springframework.context.annotation.Scope("singleton")
public class AdvancedServicePricingStrategy implements PricingStrategy {

    // Base prices for advanced services (in Sri Lankan Rupees - LKR)
    private static final BigDecimal ENGINE_REPAIR_PRICE = new BigDecimal("15000.00");
    private static final BigDecimal TRANSMISSION_SERVICE_PRICE = new BigDecimal("25000.00");
    private static final BigDecimal BRAKE_SYSTEM_REPAIR_PRICE = new BigDecimal("12000.00");
    private static final BigDecimal ELECTRICAL_SYSTEM_REPAIR_PRICE = new BigDecimal("8000.00");
    private static final BigDecimal AC_SYSTEM_REPAIR_PRICE = new BigDecimal("10000.00");
    private static final BigDecimal MAJOR_OVERHAUL_PRICE = new BigDecimal("35000.00");
    private static final BigDecimal DEFAULT_ADVANCED_PRICE = new BigDecimal("15000.00");

    // No additional charges for advanced services

    @Override
    public BigDecimal calculateBasePrice(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return DEFAULT_ADVANCED_PRICE;
        }

        switch (serviceType.toLowerCase().trim()) {
            case "engine inspection":
            case "engine repair":
            case "engine overhaul":
                return ENGINE_REPAIR_PRICE;
            case "transmission service":
            case "transmission repair":
            case "transmission overhaul":
                return TRANSMISSION_SERVICE_PRICE;
            case "brake service":
            case "brake repair":
            case "brake system":
                return BRAKE_SYSTEM_REPAIR_PRICE;
            case "electrical service":
            case "electrical repair":
            case "electrical system":
            case "wiring repair":
                return ELECTRICAL_SYSTEM_REPAIR_PRICE;
            case "ac service":
            case "ac repair":
            case "air conditioning":
                return AC_SYSTEM_REPAIR_PRICE;
            case "major overhaul":
            case "complete overhaul":
                return MAJOR_OVERHAUL_PRICE;
            default:
                return DEFAULT_ADVANCED_PRICE;
        }
    }

    @Override
    public BigDecimal calculateAdditionalCharges(String serviceType, BigDecimal basePrice) {
        // No additional charges for advanced services
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateTotalPrice(String serviceType, BigDecimal basePrice, BigDecimal additionalCharges) {
        return basePrice.add(additionalCharges);
    }

    @Override
    public String getServiceCategory() {
        return "ADVANCED_SERVICE";
    }

    @Override
    public boolean appliesTo(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return false;
        }

        String lowerServiceType = serviceType.toLowerCase().trim();
        return lowerServiceType.contains("engine inspection") ||
                lowerServiceType.contains("engine repair") ||
                lowerServiceType.contains("transmission") ||
                lowerServiceType.contains("brake service") ||
                lowerServiceType.contains("electrical service") ||
                lowerServiceType.contains("ac service") ||
                lowerServiceType.contains("overhaul") ||
                lowerServiceType.contains("major repair") ||
                lowerServiceType.contains("diagnostic");
    }
}
