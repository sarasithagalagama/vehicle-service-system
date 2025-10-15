package com.vehicleservice.service;

// Import statements for booking service functionality
import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.Booking.PaymentStatus;
import com.vehicleservice.repository.BookingRepository;
import com.vehicleservice.strategy.PricingStrategyManager;
import com.vehicleservice.strategy.SlotGenerationStrategyManager;
import com.vehicleservice.strategy.PaymentProcessingStrategyManager;
import com.vehicleservice.strategy.PaymentProcessingStrategy;
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

// Service class for booking management operations
@Service
@org.springframework.context.annotation.Scope("singleton")
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PricingStrategyManager pricingStrategyManager;

    @Autowired
    private SlotGenerationStrategyManager slotGenerationStrategyManager;

    @Autowired
    private PaymentProcessingStrategyManager paymentProcessingStrategyManager;

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
        // Generate unique booking number: BK + 6-digit random number
        Random random = new Random();
        String bookingNumber;
        int attempts = 0;

        do {
            int randomNumber = random.nextInt(900000) + 100000; // 100000 to 999999
            bookingNumber = "BK" + randomNumber;
            attempts++;
        } while (bookingRepository.existsByBookingNumber(bookingNumber) && attempts < 10);

        // If we couldn't find a unique number after 10 attempts, use timestamp as
        // fallback
        if (attempts >= 10) {
            bookingNumber = "BK" + System.currentTimeMillis();
        }

        return bookingNumber;
    }

    // =================== SLOT AVAILABILITY FUNCTIONALITY ===================

    // Service pricing is now handled by Strategy pattern

    // Maximum bookings per slot
    private static final int MAX_BOOKINGS_PER_SLOT = 2;

    /**
     * Get available time slots for a given date
     */
    public List<TimeSlot> getAvailableSlots(LocalDate date) {
        return getAvailableSlots(date, null);
    }

    /**
     * Get available time slots for a given date and service type using Strategy
     * pattern
     */
    public List<TimeSlot> getAvailableSlots(LocalDate date, String serviceType) {
        List<TimeSlot> availableSlots = new ArrayList<>();

        // Generate slots using Strategy pattern
        List<TimeSlot> allSlots = slotGenerationStrategyManager.generateSlots(date, serviceType);

        // Get existing bookings for the date
        List<Booking> existingBookings = bookingRepository.findByBookingDate(date);

        // Check availability for each slot using Strategy pattern
        int maxBookingsPerSlot = slotGenerationStrategyManager.getMaxBookingsPerSlot(serviceType);

        for (TimeSlot slot : allSlots) {
            int bookingCount = countBookingsInSlot(existingBookings, slot);
            if (bookingCount < maxBookingsPerSlot) {
                slot.setAvailable(true);
                slot.setRemainingSlots(maxBookingsPerSlot - bookingCount);
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
        TimeSlot requestedSlot = new TimeSlot(date, time, time.plusMinutes(60)); // 60 minutes default slot duration

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

        // Generate slots using Strategy pattern
        List<TimeSlot> allSlots = slotGenerationStrategyManager.generateSlots(date, serviceType);
        List<TimeSlot> availableSlots = new ArrayList<>();

        // Check availability for each slot with current data using Strategy pattern
        int maxBookingsPerSlot = slotGenerationStrategyManager.getMaxBookingsPerSlot(serviceType);
        for (TimeSlot slot : allSlots) {
            int bookingCount = countBookingsInSlot(currentBookings, slot);
            if (bookingCount < maxBookingsPerSlot) {
                slot.setAvailable(true);
                slot.setRemainingSlots(maxBookingsPerSlot - bookingCount);
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
     * Get fixed price for a service type using Strategy pattern
     */
    public double getServicePrice(String serviceType) {
        BigDecimal basePrice = pricingStrategyManager.calculateBasePrice(serviceType);
        return basePrice.doubleValue();
    }

    /**
     * Get service price as BigDecimal using Strategy pattern
     */
    public BigDecimal getServicePriceAsBigDecimal(String serviceType) {
        return pricingStrategyManager.calculateBasePrice(serviceType);
    }

    /**
     * Get complete pricing information using Strategy pattern
     */
    public PricingStrategyManager.PricingResult getCompletePricing(String serviceType) {
        return pricingStrategyManager.calculateCompletePricing(serviceType);
    }

    /**
     * Calculate total cost including additional charges using Strategy pattern
     */
    public double calculateTotalCost(String serviceType, double additionalCharges) {
        BigDecimal basePrice = pricingStrategyManager.calculateBasePrice(serviceType);
        BigDecimal additionalChargesBD = BigDecimal.valueOf(additionalCharges);
        BigDecimal totalPrice = pricingStrategyManager.calculateTotalPrice(serviceType, basePrice, additionalChargesBD);
        return totalPrice.doubleValue();
    }

    /**
     * Calculate total cost as BigDecimal using Strategy pattern
     */
    public BigDecimal calculateTotalCostAsBigDecimal(String serviceType, BigDecimal additionalCharges) {
        BigDecimal basePrice = pricingStrategyManager.calculateBasePrice(serviceType);
        return pricingStrategyManager.calculateTotalPrice(serviceType, basePrice, additionalCharges);
    }

    /**
     * Calculate remaining amount to pay
     */
    public double calculateRemainingAmount(double totalCost, double paidAmount) {
        return Math.max(0, totalCost - paidAmount);
    }

    /**
     * Process payment for a booking using Strategy pattern
     */
    public PaymentProcessingStrategy.PaymentResult processPayment(
            Booking booking, BigDecimal amount, String paymentMethod) {
        return paymentProcessingStrategyManager.processPayment(booking, amount, paymentMethod);
    }

    /**
     * Calculate processing fees for a payment using Strategy pattern
     */
    public BigDecimal calculateProcessingFees(BigDecimal amount, String paymentMethod) {
        return paymentProcessingStrategyManager.calculateProcessingFees(amount, paymentMethod);
    }

    /**
     * Validate payment before processing using Strategy pattern
     */
    public PaymentProcessingStrategy.PaymentValidationResult validatePayment(
            Booking booking, BigDecimal amount, String paymentMethod) {
        return paymentProcessingStrategyManager.validatePayment(booking, amount, paymentMethod);
    }

    /**
     * Get supported payment methods
     */
    public List<String> getSupportedPaymentMethods() {
        return paymentProcessingStrategyManager.getSupportedPaymentMethods();
    }

    /**
     * Calculate service pricing using pricing strategies
     */
    public PricingStrategyManager.PricingResult calculateServicePricing(String serviceType) {
        return pricingStrategyManager.calculateCompletePricing(serviceType);
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
            // If fully paid, set remaining amount to zero
            booking.setRemainingAmount(BigDecimal.ZERO);
        } else if (paidAmount > 0) {
            booking.setPaymentStatus(PaymentStatus.PARTIAL);
            // Update remaining amount
            BigDecimal remainingAmount = booking.getTotalPrice().subtract(booking.getPaidAmount());
            booking.setRemainingAmount(remainingAmount.max(BigDecimal.ZERO));
        } else {
            booking.setPaymentStatus(PaymentStatus.PENDING);
            // Set remaining amount to total price
            booking.setRemainingAmount(booking.getTotalPrice());
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
        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public int getRemainingSlots() {
            return remainingSlots;
        }

        public void setRemainingSlots(int remainingSlots) {
            this.remainingSlots = remainingSlots;
        }

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
        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public List<TimeSlot> getSlots() {
            return slots;
        }

        public void setSlots(List<TimeSlot> slots) {
            this.slots = slots;
        }
    }
}
