package com.vehicleservice.controller;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.User;
import com.vehicleservice.entity.InventoryItem;
import com.vehicleservice.entity.InventoryTransaction;
import com.vehicleservice.service.BookingService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class StaffController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private InventoryService inventoryService;
    

    /**
     * Staff dashboard main endpoint
     */
    @GetMapping("/staff/dashboard")
    public String staffDashboardDirect(Authentication authentication, Model model,
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

            // Get filtered bookings
            List<Booking> allBookings = bookingService.getAllBookings();
            
            // Apply search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchTerm = search.toLowerCase().trim();
                allBookings = allBookings.stream()
                    .filter(booking -> 
                        (booking.getCustomerName() != null && booking.getCustomerName().toLowerCase().contains(searchTerm)) ||
                        (booking.getVehicleNumber() != null && booking.getVehicleNumber().toLowerCase().contains(searchTerm)) ||
                        (booking.getServiceType() != null && booking.getServiceType().toLowerCase().contains(searchTerm)) ||
                        (booking.getBookingNumber() != null && booking.getBookingNumber().toLowerCase().contains(searchTerm))
                    )
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply status filter
            if (status != null && !status.trim().isEmpty()) {
                allBookings = allBookings.stream()
                    .filter(booking -> booking.getPaymentStatus().name().equals(status))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply service type filter
            if (serviceType != null && !serviceType.trim().isEmpty()) {
                allBookings = allBookings.stream()
                    .filter(booking -> booking.getServiceType() != null && booking.getServiceType().equals(serviceType))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply date range filter
            if (dateFrom != null && !dateFrom.trim().isEmpty()) {
                try {
                    LocalDate fromDate = LocalDate.parse(dateFrom);
                    allBookings = allBookings.stream()
                        .filter(booking -> booking.getBookingDate() != null && 
                                   booking.getBookingDate().toLocalDate().isAfter(fromDate.minusDays(1)))
                        .collect(java.util.stream.Collectors.toList());
                } catch (Exception e) {
                    // If date parsing fails, ignore the filter
                }
            }
            
            if (dateTo != null && !dateTo.trim().isEmpty()) {
                try {
                    LocalDate toDate = LocalDate.parse(dateTo);
                    allBookings = allBookings.stream()
                        .filter(booking -> booking.getBookingDate() != null && 
                                   booking.getBookingDate().toLocalDate().isBefore(toDate.plusDays(1)))
                        .collect(java.util.stream.Collectors.toList());
                } catch (Exception e) {
                    // If date parsing fails, ignore the filter
                }
            }
            
            // Sort bookings by booking date (newest first)
            allBookings.sort((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()));
            
            // Pagination implementation
            int totalElements = allBookings.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<Booking> bookings = start < totalElements ? 
                allBookings.subList(start, end) : new java.util.ArrayList<>();
            
            boolean hasNext = page < totalPages - 1;
            boolean hasPrevious = page > 0;
            
            model.addAttribute("allBookings", bookings);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", totalElements);
            model.addAttribute("size", size);
            model.addAttribute("hasNext", hasNext);
            model.addAttribute("hasPrevious", hasPrevious);
            
            // Add filter parameters to model for form persistence
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("serviceType", serviceType);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);
            model.addAttribute("user", user);
            
            // Add CSRF token to model
            if (csrfToken != null) {
                model.addAttribute("_csrf", csrfToken);
            }
            
            // Return appropriate dashboard based on user role
            String role = user.getRole() != null ? user.getRole().getRoleName().toUpperCase() : "RECEPTIONIST";
            
            switch (role) {
                case "MANAGER":
                    return "redirect:/manager/dashboard";  // Manager should go to manager dashboard
                case "RECEPTIONIST":
                    loadReceptionistData(model, csrfToken);
                    model.addAttribute("user", user);
                    return "staff/receptionist-dashboard";
                case "TECHNICIAN":
                    loadTechnicianData(model, user);
                    return "staff/technician-dashboard";
                case "INVENTORY_MANAGER":
                    loadInventoryManagerData(model);
                    return "staff/inventory-manager-dashboard";
                case "CUSTOMER":
                    return "customer/dashboard";
                default:
                    loadReceptionistData(model, csrfToken);
                    model.addAttribute("user", user);
                    return "staff/receptionist-dashboard";
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while loading the dashboard", e);
        }
    }


    /**
     * Load receptionist-specific data
     */
    private void loadReceptionistData(Model model, CsrfToken csrfToken) {
        try {
            // Get all bookings for the main table
            List<Booking> allBookings = bookingService.getAllBookings();
            
            // Get all customers
            List<User> customers = userService.getUsersByRole("CUSTOMER");
            
            // Get today's bookings
            List<Booking> todaysBookings = bookingService.getBookingsByDateRange(
                java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)
            );
            
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
                    booking.getBookingDate().isAfter(java.time.LocalDateTime.now().minusDays(7)))
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
    
    /**
     * Load technician-specific data
     */
    private void loadTechnicianData(Model model, User technician) {
        try {
            // Get all bookings for technician
            List<Booking> allBookings = bookingService.getAllBookings();
            
            // Get today's bookings
            List<Booking> todaysBookings = bookingService.getBookingsByDateRange(
                java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59)
            );
            
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
    
    
    /**
     * Load inventory manager-specific data
     */
    private void loadInventoryManagerData(Model model) {
        try {
            // Get inventory items
            List<InventoryItem> inventoryItems = inventoryService.getAllInventoryItems();
            model.addAttribute("inventoryItems", inventoryItems);
            
            // Get low stock items using the service method
            List<InventoryItem> lowStockAlerts = inventoryService.getItemsNeedingReorder();
            model.addAttribute("lowStockAlerts", lowStockAlerts);
            
            // Get recent transactions
            List<InventoryTransaction> recentTransactions = inventoryService.getAllTransactions().stream()
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
            model.addAttribute("recentTransactions", recentTransactions);
            
            // Get technicians for issuing parts
            List<User> technicians = userService.getUsersByRole("TECHNICIAN");
            model.addAttribute("technicians", technicians);
            
            // Calculate statistics
            int totalItems = inventoryItems.size();
            int inStockItems = (int) inventoryItems.stream()
                .filter(item -> item.getQuantity() > item.getReorderLevel())
                .count();
            int lowStockItems = (int) inventoryItems.stream()
                .filter(item -> item.getQuantity() > 0 && item.getQuantity() <= item.getReorderLevel())
                .count();
            int outOfStockItems = (int) inventoryItems.stream()
                .filter(item -> item.getQuantity() == 0)
                .count();
            
            BigDecimal totalValue = inventoryItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("inStockItems", inStockItems);
            model.addAttribute("lowStockItems", lowStockItems);
            model.addAttribute("outOfStockItems", outOfStockItems);
            model.addAttribute("totalInventoryValue", totalValue);
            
        } catch (Exception e) {
            // Set default values
            model.addAttribute("inventoryItems", new java.util.ArrayList<>());
            model.addAttribute("lowStockAlerts", new java.util.ArrayList<>());
            model.addAttribute("recentTransactions", new java.util.ArrayList<>());
            model.addAttribute("technicians", new java.util.ArrayList<>());
            model.addAttribute("totalItems", 0);
            model.addAttribute("inStockItems", 0);
            model.addAttribute("lowStockItems", 0);
            model.addAttribute("outOfStockItems", 0);
            model.addAttribute("totalInventoryValue", BigDecimal.ZERO);
        }
    }

    // =================== BOOKING CRUD OPERATIONS ===================
    
    /**
     * Create a new booking
     */
    @PostMapping("/staff/bookings")
    public String createBooking(@RequestParam String customerName,
                               @RequestParam String vehicleNumber,
                               @RequestParam String serviceType,
                               @RequestParam String bookingDate,
                               @RequestParam(required = false) String servicePrice,
                               @RequestParam(required = false) String additionalCharges,
                               @RequestParam(required = false) String notes,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            // Create new booking
            Booking booking = new Booking();
            booking.setCustomerName(customerName);
            booking.setVehicleNumber(vehicleNumber);
            booking.setServiceType(serviceType);
            booking.setBookingDate(java.time.LocalDateTime.parse(bookingDate));
            booking.setNotes(notes);
            
            // Set prices
            if (servicePrice != null && !servicePrice.isEmpty()) {
                booking.setServicePrice(new java.math.BigDecimal(servicePrice));
            }
            if (additionalCharges != null && !additionalCharges.isEmpty()) {
                booking.setAdditionalCharges(new java.math.BigDecimal(additionalCharges));
            }
            
            // Calculate total price
            booking.calculateTotalPrice();
            
            // Set default status
            booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
            booking.setCreatedAt(java.time.LocalDateTime.now());
            booking.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Generate booking number
            booking.setBookingNumber("BK" + System.currentTimeMillis());
            
            // Save booking
            bookingService.saveBooking(booking);
            
            redirectAttributes.addFlashAttribute("success", "Booking created successfully!");
            return "redirect:/staff/dashboard";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating booking: " + e.getMessage());
            return "redirect:/staff/dashboard";
        }
    }

    /**
     * Delete a booking
     */
    @DeleteMapping("/staff/bookings/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteBooking(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (bookingOpt.isPresent()) {
                bookingService.deleteBooking(id);
                return ResponseEntity.ok("Booking deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting booking: " + e.getMessage());
        }
    }

    /**
     * Get booking details
     */
    @GetMapping("/staff/bookings/{id}")
    @ResponseBody
    public ResponseEntity<Booking> getBooking(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (bookingOpt.isPresent()) {
                return ResponseEntity.ok(bookingOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a booking
     */
    @PutMapping("/staff/bookings/{id}")
    @ResponseBody
    public ResponseEntity<String> updateBooking(@PathVariable Long id,
                                               @RequestParam(required = false) String customerName,
                                               @RequestParam(required = false) String vehicleNumber,
                                               @RequestParam(required = false) String serviceType,
                                               @RequestParam(required = false) String bookingDate,
                                               @RequestParam(required = false) String servicePrice,
                                               @RequestParam(required = false) String additionalCharges,
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
                    booking.setBookingDate(java.time.LocalDateTime.parse(bookingDate));
                }
                if (servicePrice != null && !servicePrice.isEmpty()) {
                    booking.setServicePrice(new java.math.BigDecimal(servicePrice));
                }
                if (additionalCharges != null && !additionalCharges.isEmpty()) {
                    booking.setAdditionalCharges(new java.math.BigDecimal(additionalCharges));
                }
                if (notes != null) {
                    booking.setNotes(notes);
                }
                
                // Recalculate total price
                booking.calculateTotalPrice();
                booking.setUpdatedAt(java.time.LocalDateTime.now());
                
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

    // =================== CUSTOMER MANAGEMENT ===================
    
    /**
     * Create a new customer
     */
    @PostMapping("/staff/customers/create")
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
                                RedirectAttributes redirectAttributes) {
        try {
            // Check if username or email already exists
            if (userService.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/staff/dashboard";
            }
            
            if (userService.existsByEmail(email)) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/staff/dashboard";
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
            return "redirect:/staff/dashboard";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating customer: " + e.getMessage());
            return "redirect:/staff/dashboard";
        }
    }

    // =================== SLOT AVAILABILITY ===================
    
    /**
     * Get available slots for a specific date (real-time)
     */
    @GetMapping("/staff/slots/available")
    @ResponseBody
    public ResponseEntity<?> getAvailableSlots(@RequestParam String date, @RequestParam(required = false) String serviceType) {
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            List<BookingService.TimeSlot> availableSlots = bookingService.getRealTimeAvailableSlots(bookingDate, serviceType);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching available slots: " + e.getMessage());
        }
    }

    /**
     * Get available slots for the next week
     */
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

        /**
         * Check if a specific slot is available
         */
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

    /**
     * Force refresh slot availability
     */
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
                "currency", "LKR"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching service pricing: " + e.getMessage());
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
                "currency", "LKR"
            ));
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
                        "refundAmount", booking.getPaidAmount().doubleValue()
                    ));
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "error", "Booking not found"
                    ));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to process refund: " + e.getMessage()
                ));
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
                        "paymentStatus", booking.getPaymentStatus().toString()
                    ));
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "error", "Booking not found"
                    ));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to cancel booking: " + e.getMessage()
                ));
            }
        }

    }