package com.vehicleservice.strategy.impl;

import com.vehicleservice.service.BookingService.TimeSlot;
import com.vehicleservice.strategy.SlotGenerationStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspection Service Slot Generation Strategy
 * 
 * Generates time slots for inspection and diagnostic services like
 * safety inspections, emissions tests, and vehicle diagnostics.
 * These services typically take 45-90 minutes.
 * 
 * @author Vehicle Service System
 * @version 1.0
 */
@Component
@org.springframework.context.annotation.Scope("singleton")
public class InspectionServiceSlotStrategy implements SlotGenerationStrategy {

    // Inspection service configuration
    private static final int SLOT_DURATION_MINUTES = 60; // 1 hour
    private static final int MAX_BOOKINGS_PER_SLOT = 2; // Can handle 2 inspections per slot
    private static final LocalTime WORK_START = LocalTime.of(9, 0); // 9:00 AM
    private static final LocalTime WORK_END = LocalTime.of(16, 0); // 4:00 PM

    @Override
    public List<TimeSlot> generateSlots(LocalDate date, String serviceType) {
        List<TimeSlot> slots = new ArrayList<>();

        LocalTime currentTime = WORK_START;

        while (currentTime.isBefore(WORK_END)) {
            LocalTime endTime = currentTime.plusMinutes(SLOT_DURATION_MINUTES);

            // Don't create slots that would go beyond work hours
            if (endTime.isAfter(WORK_END)) {
                break;
            }

            TimeSlot slot = new TimeSlot(date, currentTime, endTime);
            slot.setAvailable(true);
            slot.setRemainingSlots(MAX_BOOKINGS_PER_SLOT);
            slots.add(slot);

            currentTime = currentTime.plusMinutes(SLOT_DURATION_MINUTES);
        }

        return slots;
    }

    @Override
    public int getSlotDuration(String serviceType) {
        return SLOT_DURATION_MINUTES;
    }

    @Override
    public int getMaxBookingsPerSlot(String serviceType) {
        return MAX_BOOKINGS_PER_SLOT;
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
        return (lowerServiceType.contains("inspection") && !lowerServiceType.equals("engine inspection")) ||
                lowerServiceType.contains("emission") ||
                lowerServiceType.contains("diagnostic") ||
                lowerServiceType.contains("safety check") ||
                lowerServiceType.contains("pre purchase") ||
                lowerServiceType.contains("insurance check") ||
                lowerServiceType.contains("annual check");
    }
}
