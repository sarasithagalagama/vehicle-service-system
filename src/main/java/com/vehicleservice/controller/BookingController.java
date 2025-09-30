package com.vehicleservice.controller;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.service.BookingService;
import com.vehicleservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    /**
     * Display all bookings with pagination
     */
    @GetMapping
    public String getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "bookingDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Booking> bookingsPage = bookingService.getAllBookings(pageable);
        
        model.addAttribute("bookings", bookingsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookingsPage.getTotalPages());
        model.addAttribute("totalElements", bookingsPage.getTotalElements());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        return "bookings/list";
    }

    /**
     * Display booking details
     */
    @GetMapping("/{id}")
    public String getBookingById(@PathVariable Long id, Model model) {
        Optional<Booking> booking = bookingService.getBookingById(id);
        if (booking.isPresent()) {
            model.addAttribute("booking", booking.get());
            return "bookings/detail";
        } else {
            return "redirect:/bookings?error=Booking not found";
        }
    }

    /**
     * Get booking details as JSON for AJAX requests
     */
    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<Booking> getBookingByIdJson(@PathVariable Long id) {
        Optional<Booking> booking = bookingService.getBookingById(id);
        if (booking.isPresent()) {
            return ResponseEntity.ok(booking.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Show create booking form
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("booking", new Booking());
        model.addAttribute("customers", userService.getUsersByRole("CUSTOMER"));
        return "bookings/create";
    }

    /**
     * Process create booking form
     */
    @PostMapping("/create")
    public String createBooking(@ModelAttribute Booking booking, 
                               @RequestParam(required = false) String _redirect,
                               RedirectAttributes redirectAttributes) {
        try {
            // Set default values
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());
            
            // Generate booking number if not provided
            if (booking.getBookingNumber() == null || booking.getBookingNumber().isEmpty()) {
                booking.setBookingNumber("BK" + System.currentTimeMillis());
            }
            
            // Calculate total price if not provided
            if (booking.getTotalPrice() == null) {
                BigDecimal servicePrice = booking.getServicePrice() != null ? booking.getServicePrice() : BigDecimal.ZERO;
                BigDecimal additionalCharges = booking.getAdditionalCharges() != null ? booking.getAdditionalCharges() : BigDecimal.ZERO;
                booking.setTotalPrice(servicePrice.add(additionalCharges));
            }
            
            Booking savedBooking = bookingService.saveBooking(booking);
            redirectAttributes.addFlashAttribute("success", "Booking created successfully!");
            
            // Redirect to dashboard if specified, otherwise to booking detail
            if (_redirect != null && _redirect.equals("/staff/dashboard")) {
                return "redirect:/staff/dashboard";
            }
            return "redirect:/bookings/" + savedBooking.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create booking: " + e.getMessage());
            if (_redirect != null && _redirect.equals("/staff/dashboard")) {
                return "redirect:/staff/dashboard";
            }
            return "redirect:/bookings/create";
        }
    }

    /**
     * Show edit booking form
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Booking> booking = bookingService.getBookingById(id);
        if (booking.isPresent()) {
            model.addAttribute("booking", booking.get());
            model.addAttribute("customers", userService.getUsersByRole("CUSTOMER"));
            return "bookings/edit";
        } else {
            return "redirect:/bookings?error=Booking not found";
        }
    }

    /**
     * Process edit booking form
     */
    @PostMapping("/{id}/edit")
    public String updateBooking(@PathVariable Long id, 
                               @ModelAttribute Booking booking,
                               @RequestParam(required = false) String timeSlot,
                               @RequestParam(required = false) String _redirect,
                               RedirectAttributes redirectAttributes) {
        try {
            Optional<Booking> existingBooking = bookingService.getBookingById(id);
            if (existingBooking.isPresent()) {
                Booking bookingToUpdate = existingBooking.get();
                
                // Update fields
                bookingToUpdate.setCustomerName(booking.getCustomerName());
                bookingToUpdate.setVehicleNumber(booking.getVehicleNumber());
                bookingToUpdate.setServiceType(booking.getServiceType());
                
                // Handle booking date with time slot
                if (timeSlot != null && !timeSlot.isEmpty()) {
                    try {
                        // Parse the combined date and time
                        bookingToUpdate.setBookingDate(LocalDateTime.parse(timeSlot));
                    } catch (Exception e) {
                        // If parsing fails, use the original booking date
                        bookingToUpdate.setBookingDate(booking.getBookingDate());
                    }
                } else {
                    bookingToUpdate.setBookingDate(booking.getBookingDate());
                }
                bookingToUpdate.setServicePrice(booking.getServicePrice());
                bookingToUpdate.setAdditionalCharges(booking.getAdditionalCharges());
                bookingToUpdate.setPaymentStatus(booking.getPaymentStatus());
                bookingToUpdate.setNotes(booking.getNotes());
                bookingToUpdate.setUpdatedAt(LocalDateTime.now());
                
                // Recalculate total price
                BigDecimal servicePrice = bookingToUpdate.getServicePrice() != null ? bookingToUpdate.getServicePrice() : BigDecimal.ZERO;
                BigDecimal additionalCharges = bookingToUpdate.getAdditionalCharges() != null ? bookingToUpdate.getAdditionalCharges() : BigDecimal.ZERO;
                bookingToUpdate.setTotalPrice(servicePrice.add(additionalCharges));
                
                bookingService.saveBooking(bookingToUpdate);
                redirectAttributes.addFlashAttribute("success", "Booking updated successfully!");
                
                // Redirect to dashboard if specified, otherwise to booking detail
                if (_redirect != null && _redirect.equals("/staff/dashboard")) {
                    return "redirect:/staff/dashboard";
                }
                return "redirect:/bookings/" + id;
            } else {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
                if (_redirect != null && _redirect.equals("/staff/dashboard")) {
                    return "redirect:/staff/dashboard";
                }
                return "redirect:/bookings";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update booking: " + e.getMessage());
            if (_redirect != null && _redirect.equals("/staff/dashboard")) {
                return "redirect:/staff/dashboard";
            }
            return "redirect:/bookings/" + id + "/edit";
        }
    }

    /**
     * Delete booking
     */
    @PostMapping("/{id}/delete")
    public String deleteBooking(@PathVariable Long id, 
                               @RequestParam(required = false) String _redirect,
                               RedirectAttributes redirectAttributes) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(id);
            if (booking.isPresent()) {
                bookingService.deleteBooking(id);
                redirectAttributes.addFlashAttribute("success", "Booking deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete booking: " + e.getMessage());
        }
        
        // Redirect to dashboard if specified, otherwise to bookings list
        if (_redirect != null && _redirect.equals("/staff/dashboard")) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/bookings";
    }

    /**
     * Update booking status
     */
    @PostMapping("/{id}/status")
    public String updateBookingStatus(@PathVariable Long id, 
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(id);
            if (booking.isPresent()) {
                Booking bookingToUpdate = booking.get();
                bookingToUpdate.setPaymentStatus(Booking.PaymentStatus.valueOf(status));
                bookingToUpdate.setUpdatedAt(LocalDateTime.now());
                bookingService.saveBooking(bookingToUpdate);
                redirectAttributes.addFlashAttribute("success", "Booking status updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update booking status: " + e.getMessage());
        }
        return "redirect:/bookings/" + id;
    }

    /**
     * Update booking status (for dashboard)
     */
    @PostMapping("/status")
    public String updateBookingStatusFromDashboard(@RequestParam Long bookingId, 
                                                 @RequestParam String status,
                                                 RedirectAttributes redirectAttributes) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            if (booking.isPresent()) {
                Booking bookingToUpdate = booking.get();
                bookingToUpdate.setPaymentStatus(Booking.PaymentStatus.valueOf(status));
                bookingToUpdate.setUpdatedAt(LocalDateTime.now());
                bookingService.saveBooking(bookingToUpdate);
                redirectAttributes.addFlashAttribute("success", "Booking status updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Booking not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update booking status: " + e.getMessage());
        }
        return "redirect:/staff/dashboard";
    }

    /**
     * Search bookings
     */
    @GetMapping("/search")
    public String searchBookings(@RequestParam String query,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        try {
            List<Booking> bookings = bookingService.searchBookings(query);
            model.addAttribute("bookings", bookings);
            model.addAttribute("query", query);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalElements", bookings.size());
            return "bookings/search";
        } catch (Exception e) {
            model.addAttribute("error", "Search failed: " + e.getMessage());
            return "bookings/search";
        }
    }

    /**
     * Get filtered bookings for staff dashboard
     */
    @GetMapping("/staff/filtered")
    @ResponseBody
    public ResponseEntity<?> getFilteredBookings(@RequestParam(required = false) String search,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String serviceType,
                                               @RequestParam(required = false) String dateFrom,
                                               @RequestParam(required = false) String dateTo,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        try {
            // Get all bookings
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
                    java.time.LocalDate fromDate = java.time.LocalDate.parse(dateFrom);
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
                    java.time.LocalDate toDate = java.time.LocalDate.parse(dateTo);
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
            
            return ResponseEntity.ok(java.util.Map.of(
                "bookings", bookings,
                "currentPage", page,
                "totalPages", totalPages,
                "totalElements", totalElements,
                "size", size,
                "hasNext", hasNext,
                "hasPrevious", hasPrevious
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error filtering bookings: " + e.getMessage());
        }
    }
}