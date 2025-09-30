package com.vehicleservice.service;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.Booking.PaymentStatus;
import com.vehicleservice.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    
    
    public List<Booking> getAllBookings() {
        try {
            return bookingRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    public Page<Booking> getAllBookings(Pageable pageable) {
        try {
            return bookingRepository.findAll(pageable);
        } catch (Exception e) {
            e.printStackTrace();
            return Page.empty();
        }
    }
    
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
    
    @Transactional
    public Booking saveBooking(Booking booking) {
        if (booking.getBookingNumber() == null || booking.getBookingNumber().isEmpty()) {
            booking.setBookingNumber(generateBookingNumber());
        }
        
        // Save the booking
        Booking savedBooking = bookingRepository.save(booking);
        
        // Clear any cached slot availability data to ensure real-time updates
        clearSlotAvailabilityCache();
        
        return savedBooking;
    }
    
    @Transactional
    public Booking updateBooking(Booking booking) {
        Booking updatedBooking = bookingRepository.save(booking);
        // Clear cache to ensure real-time updates
        clearSlotAvailabilityCache();
        return updatedBooking;
    }
    
    @Transactional
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
        // Clear cache to ensure real-time updates
        clearSlotAvailabilityCache();
    }
    
    public List<Booking> searchBookings(String keyword) {
        return bookingRepository.findByKeyword(keyword);
    }
    
    public List<Booking> getBookingsByPaymentStatus(PaymentStatus paymentStatus) {
        return bookingRepository.findByPaymentStatus(paymentStatus);
    }
    
    
    public List<Booking> getBookingsByCustomerName(String customerName) {
        return bookingRepository.findByCustomerNameContainingIgnoreCase(customerName);
    }
    
    public List<Booking> getBookingsByVehicleNumber(String vehicleNumber) {
        return bookingRepository.findByVehicleNumberContainingIgnoreCase(vehicleNumber);
    }
    
    public List<Booking> getBookingsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return bookingRepository.findByBookingDateBetween(startDate, endDate);
        } catch (Exception e) {
            System.err.println("Error getting bookings by date range: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    
    public List<Booking> getUpcomingBookings() {
        try {
            return bookingRepository.findByBookingDateBetween(LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        } catch (Exception e) {
            System.err.println("Error getting upcoming bookings: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    private String generateBookingNumber() {
        // Generate shorter booking number: BK + 6-digit random number
        Random random = new Random();
        int randomNumber = random.nextInt(900000) + 100000; // 100000 to 999999
        return "BK" + randomNumber;
    }
    
    // =================== SLOT AVAILABILITY FUNCTIONALITY ===================
    
    // Define working hours (9 AM to 6 PM)
    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);
    
    // Define slot duration based on service type (more practical durations)
    private static final int DEFAULT_SLOT_DURATION_MINUTES = 60;   // Default 1 hour slots
    private static final int QUICK_SERVICE_DURATION_MINUTES = 30;  // Oil change, basic service (30 min)
    private static final int LONG_SERVICE_DURATION_MINUTES = 120;  // Engine repair, major repair (2 hours)
    private static final int INSPECTION_DURATION_MINUTES = 60;     // Inspection, diagnostic (1 hour)
    private static final int FLEXIBLE_SERVICE_DURATION_MINUTES = 45; // Tire service, brake service (45 min)
    
    // Fixed service pricing (in Sri Lankan Rupees - LKR)
    private static final double OIL_CHANGE_PRICE = 3500.00;
    private static final double TIRE_ROTATION_PRICE = 2000.00;
    private static final double ENGINE_REPAIR_PRICE = 15000.00;
    private static final double TRANSMISSION_SERVICE_PRICE = 25000.00;
    private static final double SAFETY_INSPECTION_PRICE = 3000.00;
    private static final double EMISSIONS_TEST_PRICE = 2500.00;
    private static final double TIRE_REPLACEMENT_PRICE = 8000.00;
    private static final double BRAKE_SERVICE_PRICE = 12000.00;
    private static final double DEFAULT_SERVICE_PRICE = 5000.00;
    
    // Maximum bookings per slot
    private static final int MAX_BOOKINGS_PER_SLOT = 2;

    /**
     * Get available time slots for a given date
     */
    public List<TimeSlot> getAvailableSlots(LocalDate date) {
        return getAvailableSlots(date, null);
    }

    /**
     * Get available time slots for a given date and service type
     */
    public List<TimeSlot> getAvailableSlots(LocalDate date, String serviceType) {
        List<TimeSlot> availableSlots = new ArrayList<>();
        
        // Generate slots based on service type
        List<TimeSlot> allSlots = generateTimeSlotsForService(date, serviceType);
        
        // Get existing bookings for the date
        List<Booking> existingBookings = bookingRepository.findByBookingDate(date);
        
        // Check availability for each slot
        for (TimeSlot slot : allSlots) {
            int bookingCount = countBookingsInSlot(existingBookings, slot);
            if (bookingCount < MAX_BOOKINGS_PER_SLOT) {
                slot.setAvailable(true);
                slot.setRemainingSlots(MAX_BOOKINGS_PER_SLOT - bookingCount);
            } else {
                slot.setAvailable(false);
                slot.setRemainingSlots(0);
            }
            availableSlots.add(slot);
        }
        
        return availableSlots;
    }

    /**
     * Check if a specific time slot is available
     */
    public boolean isSlotAvailable(LocalDate date, LocalTime time) {
        List<Booking> existingBookings = bookingRepository.findByBookingDate(date);
        TimeSlot requestedSlot = new TimeSlot(date, time, time.plusMinutes(DEFAULT_SLOT_DURATION_MINUTES));
        
        int bookingCount = countBookingsInSlot(existingBookings, requestedSlot);
        return bookingCount < MAX_BOOKINGS_PER_SLOT;
    }

    /**
     * Get available slots for the next 7 days
     */
    public List<DateSlots> getAvailableSlotsForWeek(LocalDate startDate) {
        List<DateSlots> weekSlots = new ArrayList<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            List<TimeSlot> slots = getAvailableSlots(date);
            weekSlots.add(new DateSlots(date, slots));
        }
        
        return weekSlots;
    }

    /**
     * Get real-time available slots (always fresh data)
     */
    public List<TimeSlot> getRealTimeAvailableSlots(LocalDate date, String serviceType) {
        // Force fresh data by getting current bookings
        List<Booking> currentBookings = bookingRepository.findByBookingDate(date);
        System.out.println("Real-time slot check for date: " + date + ", service: " + serviceType);
        System.out.println("Found " + currentBookings.size() + " existing bookings for this date");
        
        // Generate slots based on service type
        List<TimeSlot> allSlots = generateTimeSlotsForService(date, serviceType);
        List<TimeSlot> availableSlots = new ArrayList<>();
        
        // Check availability for each slot with current data
        for (TimeSlot slot : allSlots) {
            int bookingCount = countBookingsInSlot(currentBookings, slot);
            if (bookingCount < MAX_BOOKINGS_PER_SLOT) {
                slot.setAvailable(true);
                slot.setRemainingSlots(MAX_BOOKINGS_PER_SLOT - bookingCount);
            } else {
                slot.setAvailable(false);
                slot.setRemainingSlots(0);
            }
            availableSlots.add(slot);
            
            // Debug logging
            System.out.println("Slot " + slot.getStartTime() + "-" + slot.getEndTime() + 
                             ": " + bookingCount + " bookings, " + 
                             (slot.isAvailable() ? "Available" : "Full") + 
                             " (" + slot.getRemainingSlots() + " remaining)");
        }
        
        return availableSlots;
    }

    /**
     * Clear slot availability cache (placeholder for future caching implementation)
     */
    private void clearSlotAvailabilityCache() {
        // For now, this is a placeholder
        // In the future, if we implement caching, we would clear the cache here
        // This ensures that slot availability is always calculated with fresh data
        System.out.println("Slot availability cache cleared - next request will use fresh data");
    }
    
    /**
     * Force refresh slot availability (public method for external calls)
     */
    public void forceRefreshSlotAvailability() {
        clearSlotAvailabilityCache();
        System.out.println("Slot availability force refreshed");
    }
    
    /**
     * Get fixed price for a service type
     */
    public double getServicePrice(String serviceType) {
        if (serviceType == null || serviceType.isEmpty()) {
            return DEFAULT_SERVICE_PRICE;
        }
        
        switch (serviceType.toLowerCase()) {
            case "oil change":
                return OIL_CHANGE_PRICE;
            case "tire rotation":
                return TIRE_ROTATION_PRICE;
            case "engine repair":
                return ENGINE_REPAIR_PRICE;
            case "transmission service":
                return TRANSMISSION_SERVICE_PRICE;
            case "safety inspection":
                return SAFETY_INSPECTION_PRICE;
            case "emissions test":
                return EMISSIONS_TEST_PRICE;
            case "tire replacement":
                return TIRE_REPLACEMENT_PRICE;
            case "brake service":
                return BRAKE_SERVICE_PRICE;
            default:
                return DEFAULT_SERVICE_PRICE;
        }
    }
    
    /**
     * Calculate total cost including additional charges
     */
    public double calculateTotalCost(String serviceType, double additionalCharges) {
        double servicePrice = getServicePrice(serviceType);
        return servicePrice + additionalCharges;
    }
    
    /**
     * Calculate remaining amount to pay
     */
    public double calculateRemainingAmount(double totalCost, double paidAmount) {
        return Math.max(0, totalCost - paidAmount);
    }
    
    /**
     * Update payment information for a booking
     */
    @Transactional
    public Booking updatePayment(Long bookingId, double paidAmount) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            booking.setPaidAmount(BigDecimal.valueOf(paidAmount));
            
            // Update payment status based on amounts
            updatePaymentStatus(booking);
            
            return bookingRepository.save(booking);
        }
        return null;
    }
    
    /**
     * Update payment status based on paid amount and total price
     */
    private void updatePaymentStatus(Booking booking) {
        double totalPrice = booking.getTotalPrice().doubleValue();
        double paidAmount = booking.getPaidAmount().doubleValue();
        
        if (paidAmount >= totalPrice) {
            booking.setPaymentStatus(PaymentStatus.PAID);
        } else if (paidAmount > 0) {
            booking.setPaymentStatus(PaymentStatus.PARTIAL);
        } else {
            booking.setPaymentStatus(PaymentStatus.PENDING);
        }
    }
    
    /**
     * Process refund for a cancelled booking
     */
    @Transactional
    public Booking processRefund(Long bookingId) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            
            // Set payment status to REFUNDED
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            
            // Reset paid amount to 0 (refunded)
            booking.setPaidAmount(BigDecimal.ZERO);
            
            // Update remaining amount to total price (since refunded)
            booking.setRemainingAmount(booking.getTotalPrice());
            
            return bookingRepository.save(booking);
        }
        return null;
    }
    
    /**
     * Cancel booking and process refund if needed
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, boolean processRefund) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            
            if (processRefund && booking.getPaidAmount().doubleValue() > 0) {
                // Process refund for paid amount
                return processRefund(bookingId);
            } else {
                // Just cancel without refund
                booking.setPaymentStatus(PaymentStatus.REFUNDED);
                return bookingRepository.save(booking);
            }
        }
        return null;
    }

    /**
     * Generate time slots based on service type
     */
    private List<TimeSlot> generateTimeSlotsForService(LocalDate date, String serviceType) {
        List<TimeSlot> slots = new ArrayList<>();
        
        // Define different time slots based on service type
        if (serviceType == null || serviceType.isEmpty()) {
            // Default: All slots (9 AM to 6 PM)
            slots = generateAllTimeSlots(date);
        } else {
            switch (serviceType.toLowerCase()) {
                case "oil change":
                case "basic service":
                    // Quick services: Morning slots (9 AM to 12 PM)
                    slots = generateMorningSlots(date);
                    break;
                case "engine repair":
                case "transmission service":
                case "major repair":
                    // Long services: Afternoon slots (1 PM to 6 PM)
                    slots = generateAfternoonSlots(date);
                    break;
                case "inspection":
                case "diagnostic":
                    // Inspection services: Mid-day slots (10 AM to 3 PM)
                    slots = generateMidDaySlots(date);
                    break;
                case "tire service":
                case "brake service":
                    // Tire/Brake services: Flexible slots (9 AM to 5 PM)
                    slots = generateFlexibleSlots(date);
                    break;
                default:
                    // Default: All slots
                    slots = generateAllTimeSlots(date);
                    break;
            }
        }
        
        return slots;
    }

    /**
     * Generate all time slots (9 AM to 6 PM)
     */
    private List<TimeSlot> generateAllTimeSlots(LocalDate date) {
        return generateTimeSlotsWithDuration(date, DEFAULT_SLOT_DURATION_MINUTES, WORK_START, WORK_END);
    }

    /**
     * Generate morning slots (9 AM to 12 PM) for quick services
     */
    private List<TimeSlot> generateMorningSlots(LocalDate date) {
        return generateTimeSlotsWithDuration(date, QUICK_SERVICE_DURATION_MINUTES,
            LocalTime.of(9, 0), LocalTime.of(12, 0));
    }

    /**
     * Generate afternoon slots (1 PM to 5 PM) for long services (2-hour slots)
     */
    private List<TimeSlot> generateAfternoonSlots(LocalDate date) {
        return generateTimeSlotsWithDuration(date, LONG_SERVICE_DURATION_MINUTES,
            LocalTime.of(13, 0), LocalTime.of(17, 0));
    }

    /**
     * Generate mid-day slots (9 AM to 4 PM) for inspections (1-hour slots)
     */
    private List<TimeSlot> generateMidDaySlots(LocalDate date) {
        return generateTimeSlotsWithDuration(date, INSPECTION_DURATION_MINUTES,
            LocalTime.of(9, 0), LocalTime.of(16, 0));
    }

    /**
     * Generate flexible slots (9 AM to 5 PM) for tire/brake services (45-min slots)
     */
    private List<TimeSlot> generateFlexibleSlots(LocalDate date) {
        return generateTimeSlotsWithDuration(date, FLEXIBLE_SERVICE_DURATION_MINUTES,
            LocalTime.of(9, 0), LocalTime.of(17, 0));
    }

    /**
     * Generate time slots with specific duration
     */
    private List<TimeSlot> generateTimeSlotsWithDuration(LocalDate date, int durationMinutes, 
            LocalTime startTime, LocalTime endTime) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime currentTime = startTime;
        
        while (currentTime.isBefore(endTime)) {
            LocalTime slotEndTime = currentTime.plusMinutes(durationMinutes);
            if (slotEndTime.isAfter(endTime)) {
                break;
            }
            
            slots.add(new TimeSlot(date, currentTime, slotEndTime));
            currentTime = currentTime.plusMinutes(durationMinutes);
        }
        
        return slots;
    }

    /**
     * Count bookings in a specific time slot
     */
    private int countBookingsInSlot(List<Booking> bookings, TimeSlot slot) {
        int count = 0;
        for (Booking booking : bookings) {
            if (booking.getBookingDate() != null && booking.getBookingDate().toLocalDate().equals(slot.getDate())) {
                // Check if booking time overlaps with slot
                if (isTimeOverlapping(booking, slot)) {
                    count++;
                    System.out.println("Found overlapping booking: " + booking.getBookingNumber() + 
                                     " at " + booking.getBookingDate().toLocalTime() + 
                                     " for slot " + slot.getStartTime() + "-" + slot.getEndTime());
                }
            }
        }
        return count;
    }

    /**
     * Check if a booking time overlaps with a slot
     */
    private boolean isTimeOverlapping(Booking booking, TimeSlot slot) {
        if (booking.getBookingDate() == null) {
            return false;
        }
        
        // Extract time from booking date
        LocalTime bookingTime = booking.getBookingDate().toLocalTime();
        
        // Check if booking time falls within the slot time range
        // A booking overlaps if its time is >= slot start time and < slot end time
        return !bookingTime.isBefore(slot.getStartTime()) && 
               bookingTime.isBefore(slot.getEndTime());
    }

    /**
     * TimeSlot inner class
     */
    public static class TimeSlot {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean available;
        private int remainingSlots;

        public TimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.available = false;
            this.remainingSlots = 0;
        }

        // Getters and setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        
        public int getRemainingSlots() { return remainingSlots; }
        public void setRemainingSlots(int remainingSlots) { this.remainingSlots = remainingSlots; }

        public String getFormattedTime() {
            return startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + 
                   endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
    }

    /**
     * DateSlots inner class
     */
    public static class DateSlots {
        private LocalDate date;
        private List<TimeSlot> slots;

        public DateSlots(LocalDate date, List<TimeSlot> slots) {
            this.date = date;
            this.slots = slots;
        }

        // Getters and setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public List<TimeSlot> getSlots() { return slots; }
        public void setSlots(List<TimeSlot> slots) { this.slots = slots; }
    }
}
