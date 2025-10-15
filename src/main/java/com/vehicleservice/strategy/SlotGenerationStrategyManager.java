package com.vehicleservice.strategy;

import com.vehicleservice.service.BookingService.TimeSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Slot Generation Strategy Manager
 * 
 * Manages different slot generation strategies and selects the appropriate one
 * based on the service type. This follows the Strategy pattern to provide
 * flexible time slot generation for different types of vehicle services.
 * 
 * @author Vehicle Service System
 * @version 1.0
 */
@Component
@org.springframework.context.annotation.Scope("singleton")
public class SlotGenerationStrategyManager {

    @Autowired
    private List<SlotGenerationStrategy> slotStrategies;

    /**
     * Generate time slots for a specific date and service type using the
     * appropriate strategy
     * 
     * @param date        The date for which to generate slots
     * @param serviceType The type of service
     * @return List of available time slots
     */
    public List<TimeSlot> generateSlots(LocalDate date, String serviceType) {
        SlotGenerationStrategy strategy = selectStrategy(serviceType);
        return strategy.generateSlots(date, serviceType);
    }

    /**
     * Get the slot duration for a specific service type using the appropriate
     * strategy
     * 
     * @param serviceType The type of service
     * @return Slot duration in minutes
     */
    public int getSlotDuration(String serviceType) {
        SlotGenerationStrategy strategy = selectStrategy(serviceType);
        return strategy.getSlotDuration(serviceType);
    }

    /**
     * Get the maximum number of bookings per slot for a specific service type
     * 
     * @param serviceType The type of service
     * @return Maximum bookings per slot
     */
    public int getMaxBookingsPerSlot(String serviceType) {
        SlotGenerationStrategy strategy = selectStrategy(serviceType);
        return strategy.getMaxBookingsPerSlot(serviceType);
    }

    /**
     * Get slot generation information for a service type
     * 
     * @param serviceType The type of service
     * @return Slot generation information
     */
    public SlotGenerationInfo getSlotGenerationInfo(String serviceType) {
        SlotGenerationStrategy strategy = selectStrategy(serviceType);

        return new SlotGenerationInfo(
                serviceType,
                strategy.getServiceCategory(),
                strategy.getSlotDuration(serviceType),
                strategy.getMaxBookingsPerSlot(serviceType));
    }

    /**
     * Select the appropriate slot generation strategy for the given service type
     * 
     * @param serviceType The type of service
     * @return The appropriate slot generation strategy
     */
    private SlotGenerationStrategy selectStrategy(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            // Return the first available strategy as default
            return slotStrategies.isEmpty() ? null : slotStrategies.get(0);
        }

        // Find the first strategy that applies to this service type
        for (SlotGenerationStrategy strategy : slotStrategies) {
            if (strategy.appliesTo(serviceType)) {
                return strategy;
            }
        }

        // If no specific strategy applies, return the first available strategy
        return slotStrategies.isEmpty() ? null : slotStrategies.get(0);
    }

    /**
     * Get all available slot generation strategies
     * 
     * @return List of all slot generation strategies
     */
    public List<SlotGenerationStrategy> getAllStrategies() {
        return slotStrategies;
    }

    /**
     * Get the strategy that applies to a specific service type
     * 
     * @param serviceType The service type
     * @return The applicable strategy or null if none found
     */
    public SlotGenerationStrategy getStrategyForService(String serviceType) {
        return selectStrategy(serviceType);
    }

    /**
     * Slot Generation Info class to hold slot generation information
     */
    public static class SlotGenerationInfo {
        private final String serviceType;
        private final String serviceCategory;
        private final int slotDurationMinutes;
        private final int maxBookingsPerSlot;

        public SlotGenerationInfo(String serviceType, String serviceCategory,
                int slotDurationMinutes, int maxBookingsPerSlot) {
            this.serviceType = serviceType;
            this.serviceCategory = serviceCategory;
            this.slotDurationMinutes = slotDurationMinutes;
            this.maxBookingsPerSlot = maxBookingsPerSlot;
        }

        // Getters
        public String getServiceType() {
            return serviceType;
        }

        public String getServiceCategory() {
            return serviceCategory;
        }

        public int getSlotDurationMinutes() {
            return slotDurationMinutes;
        }

        public int getMaxBookingsPerSlot() {
            return maxBookingsPerSlot;
        }

        @Override
        public String toString() {
            return String.format("SlotGenerationInfo{serviceType='%s', category='%s', duration=%d min, maxBookings=%d}",
                    serviceType, serviceCategory, slotDurationMinutes, maxBookingsPerSlot);
        }
    }
}
