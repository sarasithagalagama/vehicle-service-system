package com.vehicleservice.controller;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.User;
import com.vehicleservice.entity.Feedback;
import com.vehicleservice.service.BookingService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.FeedbackService;
import com.vehicleservice.strategy.PaymentProcessingStrategy;
import com.vehicleservice.strategy.PricingStrategyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/customer")
@org.springframework.context.annotation.Scope("singleton")
public class CustomerController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FeedbackService feedbackService;

    /// Customer Dashboard - All-in-one interface
    @GetMapping("/dashboard")
    public String customerDashboard(Authentication authentication, Model model) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            // Get customer's bookings
            List<Booking> customerBookings = bookingService
                    .getBookingsByCustomerName(currentUser.getFirstName() + " " + currentUser.getLastName());

            // Calculate statistics
            long totalBookings = customerBookings.size();
            long pendingBookings = customerBookings.stream()
                    .filter(b -> b.getPaymentStatus() != null && b.getPaymentStatus().name().equals("PENDING"))
                    .count();
            long inProgressBookings = customerBookings.stream()
                    .filter(b -> b.getPaymentStatus() != null && b.getPaymentStatus().name().equals("PARTIAL"))
                    .count();
            long completedBookings = customerBookings.stream()
                    .filter(b -> b.getPaymentStatus() != null && b.getPaymentStatus().name().equals("PAID"))
                    .count();

            // Get upcoming bookings (next 7 days)
            LocalDate today = LocalDate.now();
            LocalDate nextWeek = today.plusDays(7);
            List<Booking> upcomingBookings = customerBookings.stream()
                    .filter(b -> b.getBookingDate() != null &&
                            b.getBookingDate().toLocalDate().isAfter(today.minusDays(1)) &&
                            b.getBookingDate().toLocalDate().isBefore(nextWeek) &&
                            (b.getPaymentStatus() == null || !b.getPaymentStatus().name().equals("PAID")))
                    .sorted((b1, b2) -> b1.getBookingDate().compareTo(b2.getBookingDate()))
                    .limit(5)
                    .toList();

            // Get recent bookings (last 5)
            List<Booking> recentBookings = customerBookings.stream()
                    .sorted((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()))
                    .limit(5)
                    .toList();

            // Add attributes to model
            model.addAttribute("customerBookings", customerBookings);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("pendingBookings", pendingBookings);
            model.addAttribute("inProgressBookings", inProgressBookings);
            model.addAttribute("completedBookings", completedBookings);
            model.addAttribute("upcomingBookings", upcomingBookings);
            model.addAttribute("recentBookings", recentBookings);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("booking", new Booking()); // Add empty booking object for the form

            return "customer/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            // Ensure booking object is always available for the form
            model.addAttribute("booking", new Booking());
            return "customer/dashboard";
        }
    }

    /// Process booking form submission
    @PostMapping("/bookings/save")
    public String saveBooking(@RequestParam Map<String, String> allParams,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/dashboard";
            }

            // Create new booking object
            Booking booking = new Booking();

            // Set basic information
            booking.setCustomerName(currentUser.getFirstName() + " " + currentUser.getLastName());
            booking.setVehicleNumber(allParams.get("vehicleNumber"));
            booking.setServiceType(allParams.get("serviceType"));
            booking.setNotes(allParams.get("notes"));

            // Set payment method
            String paymentMethod = allParams.get("paymentMethod");
            if (paymentMethod != null) {
                booking.setPaymentMethod(paymentMethod);
            }

            // Convert booking date from string to LocalDateTime
            String bookingDateStr = allParams.get("bookingDate");
            if (bookingDateStr != null && !bookingDateStr.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(bookingDateStr);
                    booking.setBookingDate(date.atStartOfDay());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Invalid date format. Please select a valid date.");
                    return "redirect:/customer/dashboard";
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Please select a booking date.");
                return "redirect:/customer/dashboard";
            }

            // Booking number will be generated by BookingService if not provided

            // Set timestamps
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            // Set payment status to PENDING for customer bookings
            booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

            // Set service price based on selected service type
            String serviceType = booking.getServiceType();
            BigDecimal servicePrice = getServicePrice(serviceType);
            booking.setServicePrice(servicePrice);
            if (booking.getAdditionalCharges() == null) {
                booking.setAdditionalCharges(BigDecimal.ZERO);
            }
            if (booking.getPaidAmount() == null) {
                booking.setPaidAmount(BigDecimal.ZERO);
            }
            if (booking.getRemainingAmount() == null) {
                booking.setRemainingAmount(BigDecimal.ZERO);
            }
            if (booking.getTotalPrice() == null) {
                booking.setTotalPrice(BigDecimal.ZERO);
            }

            // Handle paying amount
            String payingAmountStr = allParams.get("payingAmount");

            if (payingAmountStr != null && !payingAmountStr.isEmpty()) {
                try {
                    BigDecimal payingAmount = new BigDecimal(payingAmountStr);
                    booking.setPaidAmount(payingAmount);

                    // Calculate processing fees if payment method is not cash
                    BigDecimal processingFees = BigDecimal.ZERO;
                    if (paymentMethod != null && !paymentMethod.equals("CASH")) {
                        processingFees = bookingService.calculateProcessingFees(payingAmount, paymentMethod);
                    }

                    // Calculate total cost including processing fees
                    BigDecimal totalCost = servicePrice.add(booking.getAdditionalCharges()).add(processingFees);
                    booking.setTotalPrice(totalCost);

                    // Calculate remaining amount using total cost (including processing fees)
                    BigDecimal remainingAmount = totalCost.subtract(payingAmount);
                    booking.setRemainingAmount(remainingAmount.max(BigDecimal.ZERO));
                } catch (NumberFormatException e) {
                    // If invalid number, set to zero
                    booking.setPaidAmount(BigDecimal.ZERO);
                    booking.setTotalPrice(servicePrice.add(booking.getAdditionalCharges()));
                    booking.setRemainingAmount(servicePrice.add(booking.getAdditionalCharges()));
                }
            } else {
                booking.setPaidAmount(BigDecimal.ZERO);
                booking.setTotalPrice(servicePrice.add(booking.getAdditionalCharges()));
                booking.setRemainingAmount(servicePrice.add(booking.getAdditionalCharges()));
            }

            // Save booking
            bookingService.saveBooking(booking);
            redirectAttributes.addFlashAttribute("message", "Booking created successfully!");

            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create booking: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    /// Update booking
    @PostMapping("/bookings/update")
    public String updateBooking(@ModelAttribute Booking booking,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/dashboard";
            }

            // Update timestamps
            booking.setUpdatedAt(LocalDateTime.now());

            // Ensure payment status remains PENDING for customer bookings
            booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

            // Recalculate total price
            BigDecimal servicePrice = booking.getServicePrice() != null ? booking.getServicePrice() : BigDecimal.ZERO;
            BigDecimal additionalCharges = booking.getAdditionalCharges() != null ? booking.getAdditionalCharges()
                    : BigDecimal.ZERO;
            booking.setTotalPrice(servicePrice.add(additionalCharges));

            // Save booking
            bookingService.saveBooking(booking);
            redirectAttributes.addFlashAttribute("message", "Booking updated successfully!");

            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update booking: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    /// Get available slots for booking form
    @GetMapping("/slots/available")
    @ResponseBody
    public ResponseEntity<?> getAvailableSlots(@RequestParam String date,
            @RequestParam(required = false) String serviceType) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            List<BookingService.TimeSlot> availableSlots = bookingService.getRealTimeAvailableSlots(bookingDate,
                    serviceType);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching available slots: " + e.getMessage());
        }
    }

    /// Get service pricing
    @GetMapping("/service-pricing")
    @ResponseBody
    public ResponseEntity<?> getServicePricing(@RequestParam String serviceType) {
        try {
            double servicePrice = bookingService.getServicePrice(serviceType);
            return ResponseEntity.ok(Map.of(
                    "serviceType", serviceType,
                    "servicePrice", servicePrice,
                    "currency", "LKR"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching service pricing: " + e.getMessage());
        }
    }

    /// View booking details
    @GetMapping("/bookings/{id}")
    @ResponseBody
    public ResponseEntity<?> viewBooking(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (!bookingOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Booking not found");
            }

            Booking booking = bookingOpt.get();

            // Check if the booking belongs to the current user
            String customerName = currentUser.getFirstName() + " " + currentUser.getLastName();
            if (!booking.getCustomerName().equals(customerName)) {
                return ResponseEntity.badRequest().body("Access denied");
            }

            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching booking: " + e.getMessage());
        }
    }

    /// Update booking
    @PostMapping("/bookings/{id}/update")
    public String updateBooking(@PathVariable Long id,
            @ModelAttribute Booking booking,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/dashboard";
            }

            Optional<Booking> existingBookingOpt = bookingService.getBookingById(id);
            if (!existingBookingOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/customer/dashboard";
            }

            Booking existingBooking = existingBookingOpt.get();

            // Check if the booking belongs to the current user
            String customerName = currentUser.getFirstName() + " " + currentUser.getLastName();
            if (!existingBooking.getCustomerName().equals(customerName)) {
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/customer/dashboard";
            }

            // Update booking fields from the bound booking object
            existingBooking.setVehicleNumber(booking.getVehicleNumber());
            existingBooking.setServiceType(booking.getServiceType());
            existingBooking.setNotes(booking.getNotes());
            existingBooking.setBookingDate(booking.getBookingDate());
            existingBooking.setServicePrice(booking.getServicePrice());
            existingBooking.setAdditionalCharges(booking.getAdditionalCharges());
            existingBooking.setPaymentMethod(booking.getPaymentMethod());

            existingBooking.setUpdatedAt(LocalDateTime.now());

            // Ensure payment status remains PENDING for customer bookings
            existingBooking.setPaymentStatus(Booking.PaymentStatus.PENDING);

            // Recalculate total price
            BigDecimal servicePrice = existingBooking.getServicePrice() != null ? existingBooking.getServicePrice()
                    : BigDecimal.ZERO;
            BigDecimal additionalCharges = existingBooking.getAdditionalCharges() != null
                    ? existingBooking.getAdditionalCharges()
                    : BigDecimal.ZERO;
            existingBooking.setTotalPrice(servicePrice.add(additionalCharges));

            // Save updated booking
            bookingService.saveBooking(existingBooking);
            redirectAttributes.addFlashAttribute("message", "Booking updated successfully!");

            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating booking: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    /// Cancel/Delete booking
    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/dashboard";
            }

            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (!bookingOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/customer/dashboard";
            }

            Booking booking = bookingOpt.get();

            // Check if the booking belongs to the current user
            String customerName = currentUser.getFirstName() + " " + currentUser.getLastName();
            if (!booking.getCustomerName().equals(customerName)) {
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/customer/dashboard";
            }

            // Check if booking can be cancelled (only PENDING bookings can be cancelled)
            if (booking.getPaymentStatus() != Booking.PaymentStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error", "Only pending bookings can be cancelled");
                return "redirect:/customer/dashboard";
            }

            // Delete the booking
            bookingService.deleteBooking(id);
            redirectAttributes.addFlashAttribute("message", "Booking cancelled successfully!");

            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cancelling booking: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    /// Check slot availability
    @GetMapping("/slots/check")
    @ResponseBody
    public ResponseEntity<?> checkSlotAvailability(@RequestParam String date, @RequestParam String time) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            LocalTime timeObj = LocalTime.parse(time);
            boolean isAvailable = bookingService.isSlotAvailable(bookingDate, timeObj);
            return ResponseEntity.ok(Map.of("available", isAvailable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking slot availability: " + e.getMessage());
        }
    }

    /// Update customer profile
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User user, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/dashboard";
            }

            // Server-side validation
            String validationError = validateUserProfile(user);
            if (validationError != null) {
                redirectAttributes.addFlashAttribute("error", validationError);
                return "redirect:/customer/dashboard";
            }

            // Check for email conflicts (excluding current user)
            if (!currentUser.getEmail().equals(user.getEmail()) &&
                    userService.getUserByEmail(user.getEmail()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email address already exists!");
                return "redirect:/customer/dashboard";
            }

            // Update user details
            currentUser.setFirstName(user.getFirstName());
            currentUser.setLastName(user.getLastName());
            currentUser.setEmail(user.getEmail());
            currentUser.setPhoneNumber(user.getPhoneNumber());
            currentUser.setAddress(user.getAddress());
            currentUser.setCity(user.getCity());
            currentUser.setState(user.getState());
            currentUser.setZipCode(user.getZipCode());
            currentUser.setDateOfBirth(user.getDateOfBirth());
            currentUser.setUpdatedAt(LocalDateTime.now());

            userService.updateUser(currentUser);
            redirectAttributes.addFlashAttribute("message", "Profile updated successfully!");

            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    /// Change customer password
    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/dashboard";
            }

            // Validate current password
            if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
                return "redirect:/customer/dashboard";
            }

            // Validate new password confirmation
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New password and confirmation do not match");
                return "redirect:/customer/dashboard";
            }

            // Validate new password strength
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long");
                return "redirect:/customer/dashboard";
            }

            // Update password
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            currentUser.setUpdatedAt(LocalDateTime.now());
            userService.updateUser(currentUser);
            redirectAttributes.addFlashAttribute("message", "Password changed successfully!");

            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error changing password: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    private BigDecimal getServicePrice(String serviceType) {
        if (serviceType == null) {
            return BigDecimal.valueOf(5000.00); // Default service price
        }

        switch (serviceType) {
            case "Oil Change":
                return BigDecimal.valueOf(3500.00);
            case "Tire Rotation":
                return BigDecimal.valueOf(2000.00);
            case "Engine Repair":
                return BigDecimal.valueOf(15000.00);
            case "Transmission Service":
                return BigDecimal.valueOf(25000.00);
            case "Safety Inspection":
                return BigDecimal.valueOf(3000.00);
            case "Emissions Test":
                return BigDecimal.valueOf(2500.00);
            case "Tire Replacement":
                return BigDecimal.valueOf(8000.00);
            case "Brake Service":
                return BigDecimal.valueOf(12000.00);
            default:
                return BigDecimal.valueOf(5000.00); // Default service price
        }
    }

    /// Validate user profile data
    private String validateUserProfile(User user) {
        // Validate first name
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            return "First name is required";
        }
        if (user.getFirstName().length() < 2 || user.getFirstName().length() > 50) {
            return "First name must be between 2 and 50 characters";
        }
        if (!user.getFirstName().matches("^[A-Za-z\\s'-]+$")) {
            return "First name can only contain letters, spaces, hyphens, and apostrophes";
        }

        // Validate last name
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            return "Last name is required";
        }
        if (user.getLastName().length() < 2 || user.getLastName().length() > 50) {
            return "Last name must be between 2 and 50 characters";
        }
        if (!user.getLastName().matches("^[A-Za-z\\s'-]+$")) {
            return "Last name can only contain letters, spaces, hyphens, and apostrophes";
        }

        // Validate email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return "Email address is required";
        }
        if (!user.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "Please enter a valid email address";
        }
        if (user.getEmail().length() > 100) {
            return "Email address must not exceed 100 characters";
        }

        // Validate phone number (optional)
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            if (!user.getPhoneNumber().matches("^[\\+]?[0-9\\s\\-\\(\\)]{7,20}$")) {
                return "Please enter a valid phone number";
            }
        }

        // Validate city (optional)
        if (user.getCity() != null && !user.getCity().trim().isEmpty()) {
            if (!user.getCity().matches("^[A-Za-z\\s]+$")) {
                return "City name can only contain letters and spaces";
            }
            if (user.getCity().length() > 50) {
                return "City name must not exceed 50 characters";
            }
        }

        // Validate state (optional)
        if (user.getState() != null && !user.getState().trim().isEmpty()) {
            if (!user.getState().matches("^[A-Za-z\\s]+$")) {
                return "State name can only contain letters and spaces";
            }
            if (user.getState().length() > 50) {
                return "State name must not exceed 50 characters";
            }
        }

        // Validate ZIP code (optional)
        if (user.getZipCode() != null && !user.getZipCode().trim().isEmpty()) {
            if (!user.getZipCode().matches("^[A-Za-z0-9\\s-]{3,10}$")) {
                return "Please enter a valid ZIP/postal code";
            }
        }

        // Validate address (optional)
        if (user.getAddress() != null && user.getAddress().length() > 200) {
            return "Address must not exceed 200 characters";
        }

        // Validate date of birth (optional)
        if (user.getDateOfBirth() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (user.getDateOfBirth().isAfter(now)) {
                return "Date of birth cannot be in the future";
            }
        }

        return null; // No validation errors
    }

    /// Show feedback form for a specific booking
    @GetMapping("/bookings/{id}/feedback")
    public String showFeedbackForm(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (!bookingOpt.isPresent()) {
                return "redirect:/customer/dashboard?error=Booking not found";
            }

            Booking booking = bookingOpt.get();

            // Check if the booking belongs to the current user
            String customerName = currentUser.getFirstName() + " " + currentUser.getLastName();
            if (!booking.getCustomerName().equals(customerName)) {
                return "redirect:/customer/dashboard?error=Access denied";
            }

            // Check if booking is completed (PAID status)
            if (booking.getPaymentStatus() != Booking.PaymentStatus.PAID) {
                return "redirect:/customer/dashboard?error=Feedback can only be submitted for completed bookings";
            }

            // Check if feedback already exists for this booking
            if (feedbackService.existsByBookingId(booking.getId())) {
                return "redirect:/customer/dashboard?error=Feedback already submitted for this booking";
            }

            Feedback feedback = new Feedback();
            feedback.setBooking(booking);
            feedback.setUser(currentUser);

            model.addAttribute("feedback", feedback);
            model.addAttribute("booking", booking);
            model.addAttribute("currentUser", currentUser);

            return "feedback/feedback-form";
        } catch (Exception e) {
            return "redirect:/customer/dashboard?error=Error loading feedback form: " + e.getMessage();
        }
    }

    /// Submit feedback for a booking
    @PostMapping("/bookings/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/dashboard";
            }

            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (!bookingOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/customer/dashboard";
            }

            Booking booking = bookingOpt.get();

            // Check if the booking belongs to the current user
            String customerName = currentUser.getFirstName() + " " + currentUser.getLastName();
            if (!booking.getCustomerName().equals(customerName)) {
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/customer/dashboard";
            }

            // Check if booking is completed (PAID status)
            if (booking.getPaymentStatus() != Booking.PaymentStatus.PAID) {
                redirectAttributes.addFlashAttribute("error", "Feedback can only be submitted for completed bookings");
                return "redirect:/customer/dashboard";
            }

            // Check if feedback already exists for this booking
            if (feedbackService.existsByBookingId(booking.getId())) {
                redirectAttributes.addFlashAttribute("error", "Feedback already submitted for this booking");
                return "redirect:/customer/dashboard";
            }

            // Validate rating
            if (rating == null || rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5");
                return "redirect:/customer/dashboard";
            }

            // Create feedback
            Feedback feedback = new Feedback();
            feedback.setRating(rating);
            feedback.setComment(comment);
            feedback.setBooking(booking);
            feedback.setUser(currentUser);

            feedbackService.addFeedback(feedback);
            redirectAttributes.addFlashAttribute("message",
                    "Thank you for your feedback! Your review has been submitted successfully.");

            return "redirect:/customer/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error submitting feedback: " + e.getMessage());
            return "redirect:/customer/dashboard";
        }
    }

    /// Check if feedback exists for a booking
    @GetMapping("/bookings/{id}/feedback/exists")
    @ResponseBody
    public ResponseEntity<?> checkFeedbackExists(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (!bookingOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Booking not found");
            }

            Booking booking = bookingOpt.get();

            // Check if the booking belongs to the current user
            String customerName = currentUser.getFirstName() + " " + currentUser.getLastName();
            if (!booking.getCustomerName().equals(customerName)) {
                return ResponseEntity.badRequest().body("Access denied");
            }

            boolean exists = feedbackService.existsByBookingId(booking.getId());
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking feedback: " + e.getMessage());
        }
    }

    /// View all feedbacks by the current customer
    @GetMapping("/feedbacks")
    public String viewMyFeedbacks(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            List<Feedback> myFeedbacks = feedbackService.getFeedbacksByUserId(currentUser.getId());
            model.addAttribute("feedbacks", myFeedbacks);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("pageTitle", "My Feedback");

            return "feedback/feedback-list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading feedbacks: " + e.getMessage());
            return "feedback/feedback-list";
        }
    }

    /// View specific feedback details
    @GetMapping("/feedbacks/{id}")
    public String viewFeedback(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);
            if (!feedbackOpt.isPresent()) {
                return "redirect:/customer/feedbacks?error=Feedback not found";
            }

            Feedback feedback = feedbackOpt.get();

            // Check if the feedback belongs to the current user
            if (!feedback.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/customer/feedbacks?error=Access denied";
            }

            model.addAttribute("feedback", feedback);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("pageTitle", "Feedback Details");

            return "feedback/feedback-details";
        } catch (Exception e) {
            return "redirect:/customer/feedbacks?error=Error loading feedback: " + e.getMessage();
        }
    }

    /// Show edit feedback form
    @GetMapping("/feedbacks/{id}/edit")
    public String editFeedbackForm(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);
            if (!feedbackOpt.isPresent()) {
                return "redirect:/customer/feedbacks?error=Feedback not found";
            }

            Feedback feedback = feedbackOpt.get();

            // Check if the feedback belongs to the current user
            if (!feedback.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/customer/feedbacks?error=Access denied";
            }

            model.addAttribute("feedback", feedback);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("pageTitle", "Edit Feedback");

            return "feedback/feedback-edit";
        } catch (Exception e) {
            return "redirect:/customer/feedbacks?error=Error loading feedback: " + e.getMessage();
        }
    }

    /// Update feedback
    @PostMapping("/feedbacks/{id}/edit")
    public String updateFeedback(@PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/feedbacks";
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);
            if (!feedbackOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Feedback not found");
                return "redirect:/customer/feedbacks";
            }

            Feedback feedback = feedbackOpt.get();

            // Check if the feedback belongs to the current user
            if (!feedback.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/customer/feedbacks";
            }

            // Validate rating
            if (rating == null || rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5");
                return "redirect:/customer/feedbacks/" + id + "/edit";
            }

            // Update feedback
            feedback.setRating(rating);
            feedback.setComment(comment);
            feedbackService.updateFeedback(id, feedback);

            redirectAttributes.addFlashAttribute("message", "Feedback updated successfully!");
            return "redirect:/customer/feedbacks/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating feedback: " + e.getMessage());
            return "redirect:/customer/feedbacks/" + id + "/edit";
        }
    }

    /// Show delete feedback confirmation
    @GetMapping("/feedbacks/{id}/delete")
    public String deleteFeedbackForm(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);
            if (!feedbackOpt.isPresent()) {
                return "redirect:/customer/feedbacks?error=Feedback not found";
            }

            Feedback feedback = feedbackOpt.get();

            // Check if the feedback belongs to the current user
            if (!feedback.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/customer/feedbacks?error=Access denied";
            }

            model.addAttribute("feedback", feedback);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("pageTitle", "Delete Feedback");

            return "feedback/feedback-delete";
        } catch (Exception e) {
            return "redirect:/customer/feedbacks?error=Error loading feedback: " + e.getMessage();
        }
    }

    /// Delete feedback
    @PostMapping("/feedbacks/{id}/delete")
    public String deleteFeedback(@PathVariable Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/customer/feedbacks";
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);
            if (!feedbackOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Feedback not found");
                return "redirect:/customer/feedbacks";
            }

            Feedback feedback = feedbackOpt.get();

            // Check if the feedback belongs to the current user
            if (!feedback.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Access denied");
                return "redirect:/customer/feedbacks";
            }

            feedbackService.deleteFeedback(id);
            redirectAttributes.addFlashAttribute("message", "Feedback deleted successfully!");

            return "redirect:/customer/feedbacks";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting feedback: " + e.getMessage());
            return "redirect:/customer/feedbacks";
        }
    }

    /// Get supported payment methods
    @GetMapping("/payment-methods")
    @ResponseBody
    public ResponseEntity<?> getSupportedPaymentMethods() {
        try {
            List<String> paymentMethods = bookingService.getSupportedPaymentMethods();
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load payment methods: " + e.getMessage()));
        }
    }

    /// Calculate processing fees for a payment method
    @PostMapping("/payment/calculate-fees")
    @ResponseBody
    public ResponseEntity<?> calculateProcessingFees(@RequestParam BigDecimal amount,
            @RequestParam String paymentMethod) {
        try {
            BigDecimal processingFees = bookingService.calculateProcessingFees(amount, paymentMethod);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentMethod", paymentMethod,
                    "amount", amount,
                    "processingFees", processingFees,
                    "totalAmount", amount.add(processingFees)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to calculate processing fees: " + e.getMessage()));
        }
    }

    /// Calculate service pricing using pricing strategies
    @PostMapping("/pricing/calculate")
    @ResponseBody
    public ResponseEntity<?> calculateServicePricing(@RequestParam String serviceType) {
        try {
            PricingStrategyManager.PricingResult result = bookingService.calculateServicePricing(serviceType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "serviceType", result.getServiceType(),
                    "serviceCategory", result.getServiceCategory(),
                    "basePrice", result.getBasePrice(),
                    "additionalCharges", result.getAdditionalCharges(),
                    "totalPrice", result.getTotalPrice()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to calculate pricing: " + e.getMessage()));
        }
    }

    /// Process payment for a booking
    @PostMapping("/payment/process")
    @ResponseBody
    public ResponseEntity<?> processPayment(@RequestParam Long bookingId,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentMethod) {
        try {
            Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Booking not found"));
            }

            Booking booking = bookingOpt.get();
            PaymentProcessingStrategy.PaymentResult result = bookingService.processPayment(booking, amount,
                    paymentMethod);

            if (result.isSuccess()) {
                // Update booking with payment method
                booking.setPaymentMethod(paymentMethod);
                bookingService.saveBooking(booking);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.getMessage(),
                        "transactionId", result.getTransactionId(),
                        "amount", result.getProcessedAmount(),
                        "processingFees", result.getProcessingFees()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", result.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process payment: " + e.getMessage()));
        }
    }
}
