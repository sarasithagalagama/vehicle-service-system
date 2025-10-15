package com.vehicleservice.strategy;

import com.vehicleservice.service.BookingService.TimeSlot;
import java.time.LocalDate;
import java.util.List;

// Strategy pattern interface for slot generation
public interface SlotGenerationStrategy {

    // Generate time slots for a specific date and service type
    List<TimeSlot> generateSlots(LocalDate date, String serviceType);

    // Get the slot duration in minutes for this strategy
    int getSlotDuration(String serviceType);

    // Get the maximum number of bookings allowed per slot
    int getMaxBookingsPerSlot(String serviceType);

    // Get the service category for this strategy
    String getServiceCategory();

    // Check if this strategy applies to the given service type
    boolean appliesTo(String serviceType);
}
