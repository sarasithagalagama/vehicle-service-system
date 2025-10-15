package com.vehicleservice.strategy.impl;

import com.vehicleservice.service.BookingService.TimeSlot;
import com.vehicleservice.strategy.SlotGenerationStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// Strategy pattern implementation for long service slot generation
@Component
@org.springframework.context.annotation.Scope("singleton")
public class LongServiceSlotStrategy implements SlotGenerationStrategy {

    // Long service configuration
    private static final int SLOT_DURATION_MINUTES = 120; // 2 hours
    private static final int MAX_BOOKINGS_PER_SLOT = 1; // Only one long service per slot
    private static final LocalTime WORK_START = LocalTime.of(8, 0); // 8:00 AM
    private static final LocalTime WORK_END = LocalTime.of(16, 0); // 4:00 PM (allows 2-hour completion)

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

            // For long services, create slots with 30-minute gaps to allow setup/cleanup
            currentTime = currentTime.plusMinutes(SLOT_DURATION_MINUTES + 30);
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
        return "LONG_SERVICE";
    }

    @Override
    public boolean appliesTo(String serviceType) {
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return false;
        }

        String lowerServiceType = serviceType.toLowerCase().trim();
        return lowerServiceType.equals("brake service") ||
                lowerServiceType.equals("transmission service") ||
                lowerServiceType.equals("engine inspection") ||
                lowerServiceType.contains("major repair") ||
                lowerServiceType.contains("overhaul") ||
                lowerServiceType.contains("long service");
    }
}
