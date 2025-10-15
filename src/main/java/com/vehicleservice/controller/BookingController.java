package com.vehicleservice.controller;

// Import statements for booking management functionality
import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.User;
import com.vehicleservice.service.BookingService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.SingletonVerificationService;
import com.vehicleservice.util.SingletonManager;
import com.vehicleservice.strategy.PricingStrategyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Booking Controller - Handles booking-related HTTP requests
@Controller
@org.springframework.context.annotation.Scope("singleton")
public class BookingController {

    // Service dependencies for booking and user management
    // Spring's Factory pattern automatically creates and injects these dependencies
    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private SingletonVerificationService singletonVerificationService;

    @Autowired
    private SingletonManager singletonManager;

    // =================== DASHBOARD ENDPOINTS ===================

    // Receptionist dashboard
    @GetMapping("/staff/receptionist/dashboard")
    public String receptionistDashboard(Authentication authentication, Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            CsrfToken csrfToken) {
        try {
            String username = authentication != null ? authentication.getName() : null;

            if (username == null) {
                return "redirect:/login?error=Authentication failed";
            }

            User user = userService.getUserByUsername(username).orElse(null);

            if (user == null) {
                return "redirect:/login?error=User not found";
            }

            // Load receptionist-specific data
            loadReceptionistData(model, csrfToken);
            model.addAttribute("user", user);

            // Add filter parameters to model for form persistence
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("serviceType", serviceType);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);

            return "staff/receptionist-dashboard";

        } catch (Exception e) {
            throw new RuntimeException("An error occurred while loading the receptionist dashboard", e);
        }
    }

    // Load receptionist-specific data
    private void loadReceptionistData(Model model, CsrfToken csrfToken) {
        try {
            // Get all bookings for the main table
            List<Booking> allBookings = bookingService.getAllBookings();

            // Get all customers
            List<User> customers = userService.getUsersByRole("CUSTOMER");

            // Get today's bookings
            List<Booking> todaysBookings = bookingService.getBookingsByDateRange(
                    LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                    LocalDateTime.now().withHour(23).withMinute(59).withSecond(59));

            // Get upcoming bookings (next 7 days)
            List<Booking> upcomingBookings = bookingService.getUpcomingBookings();

            // Calculate statistics
            int customerCount = customers.size();
            int todayBookingsCount = todaysBookings.size();
            int pendingBookingsCount = (int) allBookings.stream()
                    .filter(booking -> booking.getPaymentStatus() == Booking.PaymentStatus.PENDING)
                    .count();
            int weekBookingsCount = (int) allBookings.stream()
                    .filter(booking -> booking.getBookingDate() != null &&
                            booking.getBookingDate().isAfter(LocalDateTime.now().minusDays(7)))
                    .count();

            // Add all attributes to model
            model.addAttribute("allBookings", allBookings);
            model.addAttribute("customers", customers);
            model.addAttribute("todaysBookings", todaysBookings);
            model.addAttribute("upcomingBookings", upcomingBookings);

            // Pagination attributes
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalBookings", allBookings.size());
            model.addAttribute("bookingsPerPage", 10);

            // Statistics
            model.addAttribute("customerCount", customerCount);
            model.addAttribute("todayBookings", todayBookingsCount);
            model.addAttribute("pendingBookings", pendingBookingsCount);
            model.addAttribute("weekBookings", weekBookingsCount);

            // Add CSRF token to model
            if (csrfToken != null) {
                model.addAttribute("_csrf", csrfToken);
            }

        } catch (Exception e) {
            // Set default values
            model.addAttribute("allBookings", new java.util.ArrayList<>());
            model.addAttribute("customers", new java.util.ArrayList<>());
            model.addAttribute("todaysBookings", new java.util.ArrayList<>());
            model.addAttribute("upcomingBookings", new java.util.ArrayList<>());
            model.addAttribute("customerCount", 0);
            model.addAttribute("todayBookings", 0);
            model.addAttribute("pendingBookings", 0);
            model.addAttribute("weekBookings", 0);
        }
    }

    //Create a new customer

    @PostMapping("/staff/receptionist/customers/create")
    public String createCustomer(@RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false, defaultValue = "true") String isActive,
            @RequestParam(required = false) String _redirect,
            Authentication authentication,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            // Check if username or email already exists
            if (userService.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/staff/receptionist/dashboard";
            }

            if (userService.existsByEmail(email)) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/staff/receptionist/dashboard";
            }

            // Get CUSTOMER role
            com.vehicleservice.entity.Role customerRole = userService.getRoleByName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Customer role not found"));

            // Create new user
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password); // Password will be encoded in UserService
            user.setPhoneNumber(phoneNumber);
            user.setAddress(address);
            user.setCity(city);
            user.setState(state);
            user.setZipCode(zipCode);
            user.setIsActive("true".equalsIgnoreCase(isActive));
            user.setRole(customerRole);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            // Set date of birth if provided
            if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                try {
                    user.setDateOfBirth(LocalDateTime.parse(dateOfBirth + "T00:00:00"));
                } catch (Exception e) {
                    // If parsing fails, leave dateOfBirth as null
                }
            }

            // Save user
            userService.saveUser(user);

            redirectAttributes.addFlashAttribute("success", "Customer created successfully!");

            // Redirect back to dashboard or specified redirect
            if (_redirect != null && !_redirect.isEmpty()) {
                return "redirect:" + _redirect;
            }
            return "redirect:/staff/receptionist/dashboard";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating customer: " + e.getMessage());
            return "redirect:/staff/receptionist/dashboard";
        }
    }

    // Technician dashboard

    @GetMapping("/staff/technician/dashboard")
    public String technicianDashboard(Authentication authentication, Model model) {
        try {
            String username = authentication != null ? authentication.getName() : null;

            if (username == null) {
                return "redirect:/login?error=Authentication failed";
            }

            User technician = userService.getUserByUsername(username).orElse(null);

            if (technician == null) {
                return "redirect:/login?error=User not found";
            }

            // Load technician-specific data
            loadTechnicianData(model, technician);
            model.addAttribute("user", technician);

            return "staff/technician-dashboard";

        } catch (Exception e) {
            throw new RuntimeException("An error occurred while loading the technician dashboard", e);
        }
    }

    // Load technician-specific data
    private void loadTechnicianData(Model model, User technician) {
        try {
            // Get all bookings for technician
            List<Booking> allBookings = bookingService.getAllBookings();

            // Get today's bookings
            List<Booking> todaysBookings = bookingService.getBookingsByDateRange(
                    LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                    LocalDateTime.now().withHour(23).withMinute(59).withSecond(59));

            // Add attributes to model
            model.addAttribute("allBookings", allBookings);
            model.addAttribute("todaysBookings", todaysBookings);
            model.addAttribute("totalBookings", allBookings.size());
            model.addAttribute("todaysBookingsCount", todaysBookings.size());

        } catch (Exception e) {
            // Set default values
            model.addAttribute("allBookings", new java.util.ArrayList<>());
            model.addAttribute("todaysBookings", new java.util.ArrayList<>());
            model.addAttribute("totalBookings", 0);
            model.addAttribute("todaysBookingsCount", 0);
        }
    }

    // =================== BOOKING CRUD OPERATIONS ===================

    // Create a new booking
    @PostMapping("/staff/bookings")
    public String createBooking(@RequestParam String customerName,
            @RequestParam String vehicleNumber,
            @RequestParam String serviceType,
            @RequestParam String bookingDate,
            @RequestParam(required = false) String servicePrice,
            @RequestParam(required = false) String additionalCharges,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String paidAmount,
            @RequestParam(required = false) String remainingAmount,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            // Create new booking
            Booking booking = new Booking();
            booking.setCustomerName(customerName);
            booking.setVehicleNumber(vehicleNumber);
            booking.setServiceType(serviceType);
            booking.setBookingDate(LocalDateTime.parse(bookingDate));
            booking.setPaymentMethod(paymentMethod);
            booking.setNotes(notes);

            // Set prices
            if (servicePrice != null && !servicePrice.isEmpty()) {
                booking.setServicePrice(new BigDecimal(servicePrice));
            }
            if (additionalCharges != null && !additionalCharges.isEmpty()) {
                booking.setAdditionalCharges(new BigDecimal(additionalCharges));
            }

            // Set paid amount and remaining amount
            if (paidAmount != null && !paidAmount.isEmpty()) {
                booking.setPaidAmount(new BigDecimal(paidAmount));
            }
            if (remainingAmount != null && !remainingAmount.isEmpty()) {
                booking.setRemainingAmount(new BigDecimal(remainingAmount));
            }

            // Calculate total price including processing fees
            booking.calculateTotalPrice();

            // Add processing fees if payment method is not cash (do this AFTER all setters)
            if (paymentMethod != null && !paymentMethod.equals("CASH")) {
                // Calculate processing fees based on service price (before adding processing
                // fees)
                BigDecimal baseAmount = booking.getServicePrice() != null ? booking.getServicePrice() : BigDecimal.ZERO;
                BigDecimal additionalChargesAmount = booking.getAdditionalCharges() != null
                        ? booking.getAdditionalCharges()
                        : BigDecimal.ZERO;
                BigDecimal amountForFeeCalculation = baseAmount.add(additionalChargesAmount);

                BigDecimal processingFees = bookingService.calculateProcessingFees(amountForFeeCalculation,
                        paymentMethod);
                booking.setTotalPrice(booking.getTotalPrice().add(processingFees));
            }

            // Update payment status based on paid amount
            if (booking.getPaidAmount() != null && booking.getTotalPrice() != null) {
                if (booking.getPaidAmount().compareTo(booking.getTotalPrice()) >= 0) {
                    booking.setPaymentStatus(Booking.PaymentStatus.PAID);
                    booking.setRemainingAmount(BigDecimal.ZERO);
                } else if (booking.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                    booking.setPaymentStatus(Booking.PaymentStatus.PARTIAL);
                    // Recalculate remaining amount
                    booking.setRemainingAmount(booking.getTotalPrice().subtract(booking.getPaidAmount()));
                } else {
                    booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
                    booking.setRemainingAmount(booking.getTotalPrice());
                }
            } else {
                // Set default status
                booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
                booking.setRemainingAmount(booking.getTotalPrice());
            }
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());

            // Save booking
            bookingService.saveBooking(booking);

            redirectAttributes.addFlashAttribute("success", "Booking created successfully!");
            return "redirect:/staff/dashboard";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating booking: " + e.getMessage());
            return "redirect:/staff/dashboard";
        }
    }

    /// Delete a booking
    @DeleteMapping("/staff/bookings/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteBooking(@PathVariable Long id, Authentication authentication) {
        try {
            System.out.println("Deleting booking with ID: " + id);
            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (bookingOpt.isPresent()) {
                System.out.println("Booking found, proceeding with deletion: " + bookingOpt.get().getBookingNumber());
                bookingService.deleteBooking(id);
                System.out.println("Booking deleted successfully");
                return ResponseEntity.ok("Booking deleted successfully");
            } else {
                System.out.println("Booking not found for deletion, ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.out.println("Error deleting booking: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error deleting booking: " + e.getMessage());
        }
    }

    // Get booking details
    @GetMapping("/staff/bookings/{id}")
    @ResponseBody
    public ResponseEntity<Booking> getBooking(@PathVariable Long id, Authentication authentication) {
        try {
            System.out.println("Fetching booking with ID: " + id);
            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (bookingOpt.isPresent()) {
                System.out.println("Booking found: " + bookingOpt.get().getBookingNumber());
                return ResponseEntity.ok(bookingOpt.get());
            } else {
                System.out.println("Booking not found for ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.out.println("Error fetching booking: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // Update a booking
    @PutMapping("/staff/bookings/{id}")
    @ResponseBody
    public ResponseEntity<String> updateBooking(@PathVariable Long id,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String vehicleNumber,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String bookingDate,
            @RequestParam(required = false) String servicePrice,
            @RequestParam(required = false) String additionalCharges,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        try {
            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();

                // Update fields if provided
                if (customerName != null && !customerName.isEmpty()) {
                    booking.setCustomerName(customerName);
                }
                if (vehicleNumber != null && !vehicleNumber.isEmpty()) {
                    booking.setVehicleNumber(vehicleNumber);
                }
                if (serviceType != null && !serviceType.isEmpty()) {
                    booking.setServiceType(serviceType);
                }
                if (bookingDate != null && !bookingDate.isEmpty()) {
                    booking.setBookingDate(LocalDateTime.parse(bookingDate));
                }
                if (servicePrice != null && !servicePrice.isEmpty()) {
                    booking.setServicePrice(new BigDecimal(servicePrice));
                }
                if (additionalCharges != null && !additionalCharges.isEmpty()) {
                    booking.setAdditionalCharges(new BigDecimal(additionalCharges));
                }
                if (paymentMethod != null) {
                    booking.setPaymentMethod(paymentMethod);
                }
                if (notes != null) {
                    booking.setNotes(notes);
                }

                // Recalculate total price
                booking.calculateTotalPrice();
                booking.setUpdatedAt(LocalDateTime.now());

                // Save updated booking
                bookingService.saveBooking(booking);

                return ResponseEntity.ok("Booking updated successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating booking: " + e.getMessage());
        }
    }

    // Edit booking - Update existing booking
    @PostMapping("/staff/bookings/{id}/edit")
    public String editBooking(@PathVariable Long id,
            @RequestParam Map<String, String> allParams,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            // Get existing booking
            Optional<Booking> existingBookingOpt = bookingService.getBookingById(id);
            if (!existingBookingOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                return "redirect:/staff/dashboard";
            }

            Booking existingBooking = existingBookingOpt.get();

            // Update booking fields
            existingBooking.setCustomerName(allParams.get("customerName"));
            existingBooking.setVehicleNumber(allParams.get("vehicleNumber"));
            existingBooking.setServiceType(allParams.get("serviceType"));
            existingBooking.setNotes(allParams.get("notes"));
            existingBooking.setPaymentMethod(allParams.get("paymentMethod"));

            // Convert booking date from string to LocalDateTime
            String bookingDateStr = allParams.get("bookingDate");
            if (bookingDateStr != null && !bookingDateStr.isEmpty()) {
                try {
                    // Parse date in YYYY-MM-DD format (HTML date input format)
                    LocalDate date = LocalDate.parse(bookingDateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                    existingBooking.setBookingDate(date.atStartOfDay());
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error",
                            "Invalid date format. Please select a valid date. Received: '" + bookingDateStr
                                    + "', Error: " + e.getMessage());
                    return "redirect:/staff/dashboard";
                }
            }

            // Update pricing
            if (allParams.get("servicePrice") != null && !allParams.get("servicePrice").isEmpty()) {
                existingBooking.setServicePrice(new BigDecimal(allParams.get("servicePrice")));
            }
            if (allParams.get("additionalCharges") != null && !allParams.get("additionalCharges").isEmpty()) {
                existingBooking.setAdditionalCharges(new BigDecimal(allParams.get("additionalCharges")));
            }
            if (allParams.get("paidAmount") != null && !allParams.get("paidAmount").isEmpty()) {
                existingBooking.setPaidAmount(new BigDecimal(allParams.get("paidAmount")));
            }

            // Update payment status
            String paymentStatus = allParams.get("paymentStatus");
            if (paymentStatus != null) {
                existingBooking.setPaymentStatus(Booking.PaymentStatus.valueOf(paymentStatus));
            }

            // Recalculate total price including processing fees
            BigDecimal servicePrice = existingBooking.getServicePrice() != null ? existingBooking.getServicePrice()
                    : BigDecimal.ZERO;
            BigDecimal additionalCharges = existingBooking.getAdditionalCharges() != null
                    ? existingBooking.getAdditionalCharges()
                    : BigDecimal.ZERO;
            BigDecimal baseTotal = servicePrice.add(additionalCharges);

            // Add processing fees if payment method is not cash
            String paymentMethod = existingBooking.getPaymentMethod();
            if (paymentMethod != null && !paymentMethod.equals("CASH")) {
                BigDecimal processingFees = bookingService.calculateProcessingFees(baseTotal, paymentMethod);
                existingBooking.setTotalPrice(baseTotal.add(processingFees));
            } else {
                existingBooking.setTotalPrice(baseTotal);
            }

            // Calculate remaining amount
            BigDecimal paidAmount = existingBooking.getPaidAmount() != null ? existingBooking.getPaidAmount()
                    : BigDecimal.ZERO;
            existingBooking.setRemainingAmount(existingBooking.getTotalPrice().subtract(paidAmount));

            existingBooking.setUpdatedAt(LocalDateTime.now());

            // Save updated booking
            bookingService.saveBooking(existingBooking);
            redirectAttributes.addFlashAttribute("message", "Booking updated successfully!");

            return "redirect:/staff/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating booking: " + e.getMessage());
            return "redirect:/staff/dashboard";
        }
    }

    // =================== SLOT AVAILABILITY ===================

    // Get available slots for a specific date (real-time)
    @GetMapping("/staff/slots/available")
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

    // Get available slots for the next week
    @GetMapping("/staff/slots/week")
    @ResponseBody
    public ResponseEntity<?> getAvailableSlotsForWeek(@RequestParam(required = false) String startDate) {
        try {
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now();
            List<BookingService.DateSlots> weekSlots = bookingService.getAvailableSlotsForWeek(start);
            return ResponseEntity.ok(weekSlots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching week slots: " + e.getMessage());
        }
    }

    // Check if a specific slot is available
    @GetMapping("/staff/slots/check")
    @ResponseBody
    public ResponseEntity<?> checkSlotAvailability(@RequestParam String date, @RequestParam String time) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            LocalTime bookingTime = LocalTime.parse(time);
            boolean isAvailable = bookingService.isSlotAvailable(bookingDate, bookingTime);
            return ResponseEntity.ok(Map.of("available", isAvailable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking slot availability: " + e.getMessage());
        }
    }

    // Force refresh slot availability
    @PostMapping("/staff/slots/refresh")
    @ResponseBody
    public ResponseEntity<?> refreshSlotAvailability() {
        try {
            bookingService.forceRefreshSlotAvailability();
            return ResponseEntity.ok("Slot availability refreshed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error refreshing slot availability: " + e.getMessage());
        }
    }

    /**
     * Get service pricing information
     */
    @GetMapping("/staff/service-pricing")
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

    /**
     * Calculate processing fees for staff
     */
    @PostMapping("/staff/payment/calculate-fees")
    @ResponseBody
    public ResponseEntity<?> calculateStaffProcessingFees(@RequestParam BigDecimal amount,
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

    /**
     * Calculate complete service pricing for staff using pricing strategies
     */
    @PostMapping("/staff/pricing/calculate")
    @ResponseBody
    public ResponseEntity<?> calculateStaffServicePricing(@RequestParam String serviceType) {
        try {
            System.out.println("Calculating pricing for service type: " + serviceType);
            PricingStrategyManager.PricingResult result = bookingService.calculateServicePricing(serviceType);
            System.out.println("Pricing result: " + result);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "serviceType", result.getServiceType(),
                    "serviceCategory", result.getServiceCategory(),
                    "basePrice", result.getBasePrice(),
                    "additionalCharges", result.getAdditionalCharges(),
                    "totalPrice", result.getTotalPrice()));
        } catch (Exception e) {
            System.err.println("Error calculating pricing for service type: " + serviceType);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to calculate pricing: " + e.getMessage()));
        }
    }

    /**
     * Calculate total cost for a booking
     */
    @PostMapping("/staff/calculate-cost")
    @ResponseBody
    public ResponseEntity<?> calculateTotalCost(@RequestParam String serviceType,
            @RequestParam(defaultValue = "0") double additionalCharges) {
        try {
            double servicePrice = bookingService.getServicePrice(serviceType);
            double totalCost = bookingService.calculateTotalCost(serviceType, additionalCharges);
            return ResponseEntity.ok(Map.of(
                    "serviceType", serviceType,
                    "servicePrice", servicePrice,
                    "additionalCharges", additionalCharges,
                    "totalCost", totalCost,
                    "currency", "LKR"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error calculating cost: " + e.getMessage());
        }
    }

    /**
     * Process refund for a booking
     */
    @PostMapping("/staff/bookings/{id}/refund")
    @ResponseBody
    public ResponseEntity<?> processRefund(@PathVariable Long id) {
        try {
            Booking booking = bookingService.processRefund(id);
            if (booking != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Refund processed successfully",
                        "bookingId", booking.getId(),
                        "paymentStatus", booking.getPaymentStatus().toString(),
                        "refundAmount", booking.getPaidAmount().doubleValue()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "error", "Booking not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to process refund: " + e.getMessage()));
        }
    }

    /**
     * Cancel booking with optional refund
     */
    @PostMapping("/staff/bookings/{id}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelBooking(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean processRefund) {
        try {
            Booking booking = bookingService.cancelBooking(id, processRefund);
            if (booking != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", processRefund ? "Booking cancelled and refund processed" : "Booking cancelled",
                        "bookingId", booking.getId(),
                        "paymentStatus", booking.getPaymentStatus().toString()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "error", "Booking not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to cancel booking: " + e.getMessage()));
        }
    }

    // =================== SINGLETON VERIFICATION DEMO ===================

    // Demo endpoint to show singleton verification
    @GetMapping("/demo/singleton-verification")
    @ResponseBody
    public Map<String, Object> demonstrateSingletonVerification() {
        System.out.println("\n=== SINGLETON VERIFICATION DEMO ===");

        // 1. Verify SingletonManager behavior
        System.out.println("1. Testing SingletonManager...");
        boolean singletonManagerResult = singletonManager.verifySingletonBehavior();
        System.out.println("   SingletonManager verification: " + (singletonManagerResult ? "PASSED ✓" : "FAILED ✗"));

        // 2. Test multiple instances of SingletonManager
        System.out.println("2. Testing multiple getInstance() calls...");
        SingletonManager instance1 = SingletonManager.getInstance();
        SingletonManager instance2 = SingletonManager.getInstance();
        boolean sameInstance = instance1 == instance2;
        System.out.println("   Instance 1: " + instance1.toString());
        System.out.println("   Instance 2: " + instance2.toString());
        System.out.println("   Same instance: " + (sameInstance ? "YES ✓" : "NO ✗"));

        // 3. Test Spring-managed singletons
        System.out.println("3. Testing Spring-managed singletons...");
        boolean bookingServiceSingleton = singletonVerificationService.isBeanSingleton("bookingService");
        boolean userServiceSingleton = singletonVerificationService.isBeanSingleton("userService");
        boolean inventoryServiceSingleton = singletonVerificationService.isBeanSingleton("inventoryService");

        System.out.println("   BookingService singleton: " + (bookingServiceSingleton ? "YES ✓" : "NO ✗"));
        System.out.println("   UserService singleton: " + (userServiceSingleton ? "YES ✓" : "NO ✗"));
        System.out.println("   InventoryService singleton: " + (inventoryServiceSingleton ? "YES ✓" : "NO ✗"));

        // 4. Get verification statistics
        System.out.println("4. Verification Statistics:");
        Map<String, Object> stats = singletonVerificationService.getVerificationStats();
        System.out.println("   Total beans verified: " + stats.get("totalBeans"));
        System.out.println("   Singleton beans: " + stats.get("singletonCount"));
        System.out.println("   Non-singleton beans: " + stats.get("nonSingletonCount"));
        System.out.println("   Singleton percentage: " + stats.get("singletonPercentage") + "%");

        // 5. Log singleton status
        System.out.println("5. SingletonManager Status:");
        singletonManager.logSingletonStatus();

        System.out.println("=== END SINGLETON VERIFICATION DEMO ===\n");

        return stats;
    }
}