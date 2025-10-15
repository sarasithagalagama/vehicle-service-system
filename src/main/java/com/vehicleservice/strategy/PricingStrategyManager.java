package com.vehicleservice.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Strategy pattern context class for pricing strategy management
@Component
@org.springframework.context.annotation.Scope("singleton")
public class PricingStrategyManager {

    @Autowired
    private List<PricingStrategy> pricingStrategies;

    // Calculate base price using appropriate strategy
    public BigDecimal calculateBasePrice(String serviceType) {
        PricingStrategy strategy = selectStrategy(serviceType);
        return strategy.calculateBasePrice(serviceType);
    }

    // Calculate additional charges using appropriate strategy
    public BigDecimal calculateAdditionalCharges(String serviceType, BigDecimal basePrice) {
        PricingStrategy strategy = selectStrategy(serviceType);
        return strategy.calculateAdditionalCharges(serviceType, basePrice);
    }

    // Calculate total price using appropriate strategy
    public BigDecimal calculateTotalPrice(String serviceType, BigDecimal basePrice, BigDecimal additionalCharges) {
        PricingStrategy strategy = selectStrategy(serviceType);
        return strategy.calculateTotalPrice(serviceType, basePrice, additionalCharges);
    }

    // Calculate complete pricing including base price and additional charges
    public PricingResult calculateCompletePricing(String serviceType) {
        PricingStrategy strategy = selectStrategy(serviceType);

        if (strategy == null) {
            throw new RuntimeException("No pricing strategy found for service type: " + serviceType);
        }

        BigDecimal basePrice = strategy.calculateBasePrice(serviceType);
        BigDecimal additionalCharges = strategy.calculateAdditionalCharges(serviceType, basePrice);
        BigDecimal totalPrice = strategy.calculateTotalPrice(serviceType, basePrice, additionalCharges);

        return new PricingResult(
                serviceType,
                strategy.getServiceCategory(),
                basePrice,
                additionalCharges,
                totalPrice);
    }

    // Select appropriate pricing strategy for service type
    private PricingStrategy selectStrategy(String serviceType) {
        System.out.println("Selecting strategy for service type: " + serviceType);
        System.out.println("Available strategies: " + pricingStrategies.size());

        if (serviceType == null || serviceType.trim().isEmpty()) {
            // Return first available strategy as default
            System.out.println("Service type is null or empty, returning first strategy");
            return pricingStrategies.isEmpty() ? null : pricingStrategies.get(0);
        }

        // Find first strategy that applies to service type
        for (PricingStrategy strategy : pricingStrategies) {
            System.out.println(
                    "Checking strategy: " + strategy.getClass().getSimpleName() + " for service: " + serviceType);
            if (strategy.appliesTo(serviceType)) {
                System.out.println("Strategy " + strategy.getClass().getSimpleName() + " applies to " + serviceType);
                return strategy;
            }
        }

        // Return first available strategy if no specific match
        System.out.println("No specific strategy found, returning first available strategy");
        return pricingStrategies.isEmpty() ? null : pricingStrategies.get(0);
    }

    // Get all available pricing strategies
    public List<PricingStrategy> getAllStrategies() {
        return pricingStrategies;
    }

    // Get strategy that applies to specific service type
    public PricingStrategy getStrategyForService(String serviceType) {
        return selectStrategy(serviceType);
    }

    // Pricing result class to hold complete pricing information
    public static class PricingResult {
        private final String serviceType;
        private final String serviceCategory;
        private final BigDecimal basePrice;
        private final BigDecimal additionalCharges;
        private final BigDecimal totalPrice;

        public PricingResult(String serviceType, String serviceCategory,
                BigDecimal basePrice, BigDecimal additionalCharges, BigDecimal totalPrice) {
            this.serviceType = serviceType;
            this.serviceCategory = serviceCategory;
            this.basePrice = basePrice;
            this.additionalCharges = additionalCharges;
            this.totalPrice = totalPrice;
        }

        // Getter methods
        public String getServiceType() {
            return serviceType;
        }

        public String getServiceCategory() {
            return serviceCategory;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public BigDecimal getAdditionalCharges() {
            return additionalCharges;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        @Override
        public String toString() {
            return String.format(
                    "PricingResult{serviceType='%s', category='%s', basePrice=%s, additionalCharges=%s, totalPrice=%s}",
                    serviceType, serviceCategory, basePrice, additionalCharges, totalPrice);
        }
    }
}
