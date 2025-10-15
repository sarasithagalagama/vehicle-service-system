package com.vehicleservice.strategy.impl;

import com.vehicleservice.strategy.PricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Strategy pattern implementation for inspection service pricing
@Component
@org.springframework.context.annotation.Scope("singleton")
public class InspectionServicePricingStrategy implements PricingStrategy {

    // Base prices for inspection services (in Sri Lankan Rupees - LKR)
    private static final BigDecimal SAFETY_INSPECTION_PRICE = new BigDecimal("3000.00");
    private static final BigDecimal EMISSIONS_TEST_PRICE = new BigDecimal("2500.00");
    private static final BigDecimal VEHICLE_DIAGNOSTIC_PRICE = new BigDecimal("4000.00");
    private static final BigDecimal PRE_PURCHASE_INSPECTION_PRICE = new BigDecimal("5000.00");
    private static final BigDecimal INSURANCE_INSPECTION_PRICE = new BigDecimal("2000.00");
    private static final BigDecimal ANNUAL_INSPECTION_PRICE = new BigDecimal("3500.00");
    private static final BigDecimal DEFAULT_INSPECTION_PRICE = new BigDecimal("3000.00");

    // Additional charges for inspection services
    private static final BigDecimal DETAILED_REPORT_FEE = new BigDecimal("1500.00");
    private static final BigDecimal CERTIFICATION_FEE = new BigDecimal("500.00");
    private static final BigDecimal RE_INSPECTION_FEE = new BigDecimal("2000.00");

    @Override
    public BigDecimal calculateBasePrice(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return DEFAULT_INSPECTION_PRICE;
        }

        switch (serviceType.toLowerCase().trim()) {
            case "safety inspection":
            case "safety check":
                return SAFETY_INSPECTION_PRICE;
            case "emissions test":
            case "emission test":
                return EMISSIONS_TEST_PRICE;
            case "vehicle diagnostic":
            case "diagnostic":
            case "computer diagnostic":
                return VEHICLE_DIAGNOSTIC_PRICE;
            case "pre purchase inspection":
            case "pre-purchase inspection":
            case "buyer inspection":
                return PRE_PURCHASE_INSPECTION_PRICE;
            case "insurance inspection":
            case "insurance check":
                return INSURANCE_INSPECTION_PRICE;
            case "annual inspection":
            case "yearly inspection":
                return ANNUAL_INSPECTION_PRICE;
            default:
                return DEFAULT_INSPECTION_PRICE;
        }
    }

    @Override
    public BigDecimal calculateAdditionalCharges(String serviceType, BigDecimal basePrice) {
        BigDecimal additionalCharges = BigDecimal.ZERO;

        // Add detailed report fee
        if (serviceType != null && serviceType.toLowerCase().contains("detailed")) {
            additionalCharges = additionalCharges.add(DETAILED_REPORT_FEE);
        }

        // Add certification fee
        if (serviceType != null && serviceType.toLowerCase().contains("certificate")) {
            additionalCharges = additionalCharges.add(CERTIFICATION_FEE);
        }

        // Add re-inspection fee
        if (serviceType != null && serviceType.toLowerCase().contains("re-inspection")) {
            additionalCharges = additionalCharges.add(RE_INSPECTION_FEE);
        }

        return additionalCharges;
    }

    @Override
    public BigDecimal calculateTotalPrice(String serviceType, BigDecimal basePrice, BigDecimal additionalCharges) {
        return basePrice.add(additionalCharges);
    }

    @Override
    public String getServiceCategory() {
        return "INSPECTION_SERVICE";
    }

    @Override
    public boolean appliesTo(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return false;
        }

        String lowerServiceType = serviceType.toLowerCase().trim();
        return lowerServiceType.contains("inspection") ||
                lowerServiceType.contains("emission") ||
                lowerServiceType.contains("diagnostic") ||
                lowerServiceType.contains("safety check") ||
                lowerServiceType.contains("pre purchase") ||
                lowerServiceType.contains("insurance check") ||
                lowerServiceType.contains("annual check");
    }
}
