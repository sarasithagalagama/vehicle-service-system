package com.vehicleservice.controller;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.Technician;
import com.vehicleservice.entity.TechnicianAssignment;
import com.vehicleservice.entity.User;
import com.vehicleservice.entity.Feedback;
import com.vehicleservice.service.BookingService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.AssignmentService;
import com.vehicleservice.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@org.springframework.context.annotation.Scope("singleton")
public class ManagerController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/manager/dashboard")
    public String managerDashboard(Authentication authentication, Model model) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            // Get manager statistics using unified service
            List<Technician> technicians = assignmentService.getAllActiveTechnicians();
            List<Booking> allBookings = bookingService.getAllBookings();

            // Get all assignments and filter out any with missing bookings
            List<TechnicianAssignment> allAssignments = assignmentService.getAllAssignments();
            List<TechnicianAssignment> assignments = new java.util.ArrayList<>();

            for (TechnicianAssignment assignment : allAssignments) {
                try {
                    // Try to access the booking to check if it exists
                    if (assignment.getBooking() != null && assignment.getBooking().getId() != null) {
                        assignments.add(assignment);
                    }
                } catch (Exception e) {
                    // Skip assignments with missing bookings
                }
            }

            // Separate assigned and unassigned bookings
            List<Booking> unassignedBookingsList = new java.util.ArrayList<>();
            List<Booking> assignedBookingsList = new java.util.ArrayList<>();

            for (Booking booking : allBookings) {
                List<TechnicianAssignment> bookingAssignments = assignmentService
                        .getAssignmentsByBooking(booking.getId());
                if (bookingAssignments.isEmpty()) {
                    unassignedBookingsList.add(booking);
                } else {
                    assignedBookingsList.add(booking);
                }
            }

            // Calculate statistics
            int totalBookings = allBookings.size();
            int unassignedBookings = unassignedBookingsList.size();
            int activeAssignments = (int) assignments.stream()
                    .filter(assignment -> assignment.getStatus() == TechnicianAssignment.AssignmentStatus.ASSIGNED ||
                            assignment.getStatus() == TechnicianAssignment.AssignmentStatus.IN_PROGRESS)
                    .count();

            // Get assignment data for assigned bookings
            Map<Long, TechnicianAssignment> bookingAssignmentMap = new HashMap<>();
            for (Booking booking : assignedBookingsList) {
                List<TechnicianAssignment> bookingAssignments = assignmentService
                        .getAssignmentsByBooking(booking.getId());
                if (!bookingAssignments.isEmpty()) {
                    bookingAssignmentMap.put(booking.getId(), bookingAssignments.get(0));
                }
            }

            // Get available technicians for assignment dropdown
            List<User> availableTechnicians = userService.getUsersByRole("TECHNICIAN");

            // Calculate assignment analytics
            int assignedCount = (int) assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.ASSIGNED)
                    .count();
            int inProgressCount = (int) assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.IN_PROGRESS)
                    .count();
            int completedCount = (int) assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.COMPLETED)
                    .count();
            int cancelledCount = (int) assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.CANCELLED)
                    .count();

            // Calculate timeline analytics
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekStart = today.minusDays(7);
            LocalDateTime monthStart = today.minusDays(30);

            int todayAssignments = (int) assignments.stream()
                    .filter(a -> a.getAssignmentDate().isAfter(today))
                    .count();
            int weekAssignments = (int) assignments.stream()
                    .filter(a -> a.getAssignmentDate().isAfter(weekStart))
                    .count();
            int monthAssignments = (int) assignments.stream()
                    .filter(a -> a.getAssignmentDate().isAfter(monthStart))
                    .count();

            // Calculate average completion time (simplified)
            String averageCompletionTime = "2.5 hrs"; // This would be calculated from actual data

            // Add all required attributes to model
            model.addAttribute("user", currentUser);
            model.addAttribute("technicians", technicians);
            model.addAttribute("bookings", allBookings);
            model.addAttribute("assignments", assignments);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("unassignedBookings", unassignedBookings);
            model.addAttribute("activeAssignments", activeAssignments);
            model.addAttribute("unassignedBookingsList", unassignedBookingsList);
            model.addAttribute("assignedBookingsList", assignedBookingsList);
            model.addAttribute("bookingAssignmentMap", bookingAssignmentMap);
            model.addAttribute("availableTechnicians", availableTechnicians);

            // Create a map of booking ID to technician name for easy lookup
            Map<Long, String> bookingTechnicianMap = new HashMap<>();
            for (TechnicianAssignment assignment : assignments) {
                String technicianName = assignment.getTechnician().getUser().getFirstName() + " " +
                        assignment.getTechnician().getUser().getLastName();
                bookingTechnicianMap.put(assignment.getBooking().getId(), technicianName);
            }

            // Add analytics data
            model.addAttribute("assignedCount", assignedCount);
            model.addAttribute("inProgressCount", inProgressCount);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("cancelledCount", cancelledCount);
            model.addAttribute("todayAssignments", todayAssignments);
            model.addAttribute("weekAssignments", weekAssignments);
            model.addAttribute("monthAssignments", monthAssignments);
            model.addAttribute("averageCompletionTime", averageCompletionTime);
            model.addAttribute("bookingTechnicianMap", bookingTechnicianMap);

            return "manager/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading manager dashboard: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/manager/test-assignment")
    @ResponseBody
    public ResponseEntity<String> testAssignment(@RequestParam Long bookingId,
            @RequestParam Long technicianId,
            Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            Optional<Technician> technician = assignmentService.getTechnicianByUserId(technicianId);

            if (booking.isPresent() && technician.isPresent()) {
                return ResponseEntity.ok("Assignment test successful");
            }
            return ResponseEntity.badRequest().body("Booking or technician not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error testing assignment: " + e.getMessage());
        }
    }

    @PostMapping("/manager/assign-technician")
    @ResponseBody
    public ResponseEntity<String> assignTechnician(@RequestParam Long bookingId,
            @RequestParam Long technicianId,
            Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            Optional<Technician> technician = assignmentService.getTechnicianByUserId(technicianId);

            if (booking.isPresent() && technician.isPresent()) {
                // Get current user from authentication
                User currentUser = userService.getUserByUsername(authentication.getName()).orElse(null);

                // Create assignment using unified service
                assignmentService.assignTechnicianToBooking(bookingId, technicianId, currentUser, null);

                // Update booking
                booking.get().setUpdatedAt(LocalDateTime.now());
                bookingService.saveBooking(booking.get());

                return ResponseEntity.ok("Technician assigned successfully");
            }
            return ResponseEntity.badRequest().body("Booking or technician not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error assigning technician: " + e.getMessage());
        }
    }

    @PostMapping("/manager/assignments/assign")
    @ResponseBody
    public ResponseEntity<String> assignTechnicianToBooking(@RequestParam Long bookingId,
            @RequestParam Long technicianId,
            @RequestParam String assignmentDate,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            Optional<Technician> technician = assignmentService.getTechnicianById(technicianId);

            if (booking.isPresent() && technician.isPresent()) {
                // Get current user from authentication
                User currentUser = userService.getUserByUsername(authentication.getName()).orElse(null);

                if (currentUser == null) {
                    return ResponseEntity.badRequest().body("Current user not found");
                }

                // Parse assignment date
                LocalDateTime assignmentDateTime = LocalDateTime.parse(assignmentDate);

                // Create assignment using unified service
                TechnicianAssignment assignment = assignmentService.assignTechnicianToBooking(bookingId, technicianId,
                        currentUser, notes, assignmentDateTime);

                if (assignment != null) {
                    // Update booking
                    booking.get().setUpdatedAt(LocalDateTime.now());
                    bookingService.saveBooking(booking.get());

                    return ResponseEntity.ok("Technician assigned successfully");
                } else {
                    return ResponseEntity.badRequest().body("Failed to create assignment");
                }
            }
            return ResponseEntity.badRequest().body("Booking or technician not found");
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid date format: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Assignment error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error assigning technician: " + e.getMessage());
        }
    }

    @PostMapping("/manager/unassign-technician")
    @ResponseBody
    public ResponseEntity<String> unassignTechnician(@RequestParam Long bookingId,
            Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            if (booking.isPresent()) {
                // Find and delete assignment
                List<TechnicianAssignment> assignments = assignmentService
                        .getAssignmentsByBooking(booking.get().getId());
                for (TechnicianAssignment assignment : assignments) {
                    assignmentService.removeAssignment(assignment.getId());
                }

                // Update booking
                booking.get().setUpdatedAt(LocalDateTime.now());
                bookingService.saveBooking(booking.get());

                return ResponseEntity.ok("Technician unassigned successfully");
            }
            return ResponseEntity.badRequest().body("Booking not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error unassigning technician: " + e.getMessage());
        }
    }

    @PostMapping("/manager/complete-booking")
    @ResponseBody
    public ResponseEntity<String> completeBooking(@RequestParam Long bookingId,
            Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            if (booking.isPresent()) {
                // Update booking
                booking.get().setUpdatedAt(LocalDateTime.now());
                bookingService.saveBooking(booking.get());

                // Update assignment status
                List<TechnicianAssignment> assignments = assignmentService
                        .getAssignmentsByBooking(booking.get().getId());
                for (TechnicianAssignment assignment : assignments) {
                    assignmentService.completeAssignment(assignment.getId());
                }

                return ResponseEntity.ok("Booking completed successfully");
            }
            return ResponseEntity.badRequest().body("Booking not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error completing booking: " + e.getMessage());
        }
    }

    @PostMapping("/manager/update-technician")
    @ResponseBody
    public ResponseEntity<String> updateTechnician(@RequestParam Long technicianId,
            @RequestParam String name,
            @RequestParam String specialization,
            @RequestParam String phone,
            @RequestParam String email,
            Authentication authentication) {
        try {
            Optional<Technician> technicianOpt = assignmentService.getTechnicianByUserId(technicianId);
            if (technicianOpt.isPresent()) {
                Technician technician = technicianOpt.get();
                technician.setSpecialization(specialization);
                technician.getUser().setPhoneNumber(phone);
                technician.getUser().setEmail(email);
                technician.getUser().setFirstName(name.split(" ")[0]);
                technician.getUser().setLastName(name.split(" ").length > 1 ? name.split(" ")[1] : "");
                technician.getUser().setUpdatedAt(LocalDateTime.now());

                assignmentService.updateTechnician(technician.getId(), technician.getSpecialization(),
                        technician.getMaxDailyWorkload(), technician.getHourlyRate(), technician.getExperienceYears());
                return ResponseEntity.ok("Technician updated successfully");
            }
            return ResponseEntity.badRequest().body("Technician not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating technician: " + e.getMessage());
        }
    }

    @GetMapping("/manager/technician-workload")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTechnicianWorkload(Authentication authentication) {
        try {
            List<Technician> technicians = assignmentService.getAllActiveTechnicians();
            List<Map<String, Object>> workloadData = new ArrayList<>();

            for (Technician technician : technicians) {
                Map<String, Object> data = new HashMap<>();
                data.put("technicianId", technician.getId());
                data.put("name", technician.getUser().getFirstName() + " " + technician.getUser().getLastName());
                data.put("specialization", technician.getSpecialization());

                // Get current assignments
                List<TechnicianAssignment> assignments = assignmentService
                        .getAssignmentsByTechnician(technician.getId());
                long activeAssignments = assignments.stream()
                        .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.ASSIGNED)
                        .count();

                data.put("activeAssignments", activeAssignments);
                data.put("totalAssignments", assignments.size());

                // Get workload data
                data.put("currentWorkload", technician.getCurrentWorkload());
                data.put("maxWorkload", technician.getMaxDailyWorkload());

                workloadData.add(data);
            }

            return ResponseEntity.ok(workloadData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/manager/assignments")
    @ResponseBody
    public ResponseEntity<String> createAssignment(@RequestParam Long bookingId,
            @RequestParam Long technicianId,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            Optional<Technician> technician = assignmentService.getTechnicianByUserId(technicianId);

            if (booking.isPresent() && technician.isPresent()) {
                // Get current user from authentication
                User currentUser = userService.getUserByUsername(authentication.getName()).orElse(null);
                assignmentService.assignTechnicianToBooking(bookingId, technicianId, currentUser, notes);

                // Update booking
                booking.get().setUpdatedAt(LocalDateTime.now());
                bookingService.saveBooking(booking.get());

                return ResponseEntity.ok("Assignment created successfully");
            }
            return ResponseEntity.badRequest().body("Booking or technician not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating assignment: " + e.getMessage());
        }
    }

    @GetMapping("/manager/assignments")
    @ResponseBody
    public ResponseEntity<List<TechnicianAssignment>> getAllAssignments(Authentication authentication) {
        try {
            List<TechnicianAssignment> assignments = assignmentService.getAllAssignments();
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/manager/assignments/{id}")
    @ResponseBody
    public ResponseEntity<TechnicianAssignment> getAssignmentById(@PathVariable Long id,
            Authentication authentication) {
        try {
            Optional<TechnicianAssignment> assignment = assignmentService.getAssignmentById(id);
            if (assignment.isPresent()) {
                return ResponseEntity.ok(assignment.get());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/manager/assignments/{id}/status")
    @ResponseBody
    public ResponseEntity<String> updateAssignmentStatus(@PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        try {
            Optional<TechnicianAssignment> assignmentOpt = assignmentService.getAssignmentById(id);
            if (assignmentOpt.isPresent()) {
                TechnicianAssignment assignment = assignmentOpt.get();
                assignment.setStatus(TechnicianAssignment.AssignmentStatus.valueOf(status));

                assignmentService.updateAssignment(assignment);
                return ResponseEntity.ok("Assignment status updated successfully");
            }
            return ResponseEntity.badRequest().body("Assignment not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating assignment status: " + e.getMessage());
        }
    }

    @PostMapping("/manager/assignments/{id}/update")
    @ResponseBody
    public ResponseEntity<String> updateAssignment(@PathVariable Long id,
            @RequestParam String technicianId,
            @RequestParam(required = false) String assignmentDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        try {
            Optional<TechnicianAssignment> assignmentOpt = assignmentService.getAssignmentById(id);

            if (assignmentOpt.isPresent()) {
                TechnicianAssignment assignment = assignmentOpt.get();

                // Update technician if provided and valid
                if (technicianId != null && !technicianId.trim().isEmpty() && !technicianId.equals("N/A")) {
                    try {
                        Long techId = Long.parseLong(technicianId);
                        Optional<Technician> technician = assignmentService.getTechnicianById(techId);
                        if (technician.isPresent()) {
                            assignment.setTechnician(technician.get());
                        } else {
                            return ResponseEntity.badRequest().body("Technician not found with ID: " + technicianId);
                        }
                    } catch (NumberFormatException e) {
                        return ResponseEntity.badRequest().body("Invalid technician ID format: " + technicianId);
                    }
                }

                // Update notes if provided
                if (notes != null && !notes.trim().isEmpty()) {
                    assignment.setNotes(notes);
                }

                // Update status if provided
                if (status != null && !status.trim().isEmpty()) {
                    try {
                        assignment.setStatus(TechnicianAssignment.AssignmentStatus.valueOf(status.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body("Invalid status: " + status);
                    }
                }

                // Update assignment date if provided
                if (assignmentDate != null && !assignmentDate.trim().isEmpty()) {
                    try {
                        LocalDateTime dateTime = LocalDateTime.parse(assignmentDate);
                        assignment.setAssignmentDate(dateTime);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body("Invalid date format: " + assignmentDate);
                    }
                }

                assignment.setUpdatedAt(LocalDateTime.now());
                assignmentService.updateAssignment(assignment);
                return ResponseEntity.ok("Assignment updated successfully");
            }
            return ResponseEntity.badRequest().body("Assignment not found with ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating assignment: " + e.getMessage());
        }
    }

    @DeleteMapping("/manager/assignments/booking/{bookingId}")
    @ResponseBody
    public ResponseEntity<String> deleteAssignmentsByBooking(@PathVariable Long bookingId,
            Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(bookingId);
            if (booking.isPresent()) {
                assignmentService.deleteAssignmentsByBooking(booking.get().getId());
                return ResponseEntity.ok("Assignments deleted successfully");
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting assignments: " + e.getMessage());
        }
    }

    @DeleteMapping("/manager/bookings/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteBookingByManager(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<Booking> booking = bookingService.getBookingById(id);
            if (booking.isPresent()) {
                // Delete related assignments first
                assignmentService.deleteAssignmentsByBooking(booking.get().getId());

                // Delete booking
                bookingService.deleteBooking(id);
                return ResponseEntity.ok("Booking deleted successfully");
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting booking: " + e.getMessage());
        }
    }

    @GetMapping("/manager/assignments/export")
    @ResponseBody
    public ResponseEntity<String> exportAssignments(Authentication authentication) {
        try {
            List<TechnicianAssignment> assignments = assignmentService.getAllAssignments();
            StringBuilder csv = new StringBuilder();
            csv.append("Assignment ID,Booking Number,Customer,Technician,Assigned By,Assignment Date,Status,Notes\n");

            for (TechnicianAssignment assignment : assignments) {
                csv.append(assignment.getId()).append(",");
                csv.append(assignment.getBooking().getBookingNumber()).append(",");
                csv.append(assignment.getBooking().getCustomerName()).append(",");
                csv.append(assignment.getTechnician().getUser().getFirstName() + " "
                        + assignment.getTechnician().getUser().getLastName()).append(",");
                csv.append(assignment.getAssignedBy().getFirstName() + " " + assignment.getAssignedBy().getLastName())
                        .append(",");
                csv.append(assignment.getAssignmentDate().toString()).append(",");
                csv.append(assignment.getStatus().toString()).append(",");
                csv.append(assignment.getNotes() != null ? assignment.getNotes() : "").append("\n");
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=assignments.csv")
                    .body(csv.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error exporting assignments: " + e.getMessage());
        }
    }

    @PostMapping("/manager/cleanup-orphaned-assignments")
    @ResponseBody
    public ResponseEntity<String> cleanupOrphanedAssignments(Authentication authentication) {
        try {
            int cleanedCount = assignmentService.cleanupOrphanedAssignments();
            return ResponseEntity.ok("Cleaned up " + cleanedCount + " orphaned assignments");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error cleaning up assignments: " + e.getMessage());
        }
    }

    // ==================== UNIFIED TECHNICIAN-ASSIGNMENT MANAGEMENT

    /// Get all technicians with their assignments
    @GetMapping("/manager/technicians-with-assignments")
    @ResponseBody
    public ResponseEntity<List<AssignmentService.TechnicianWithAssignments>> getAllTechniciansWithAssignments(
            Authentication authentication) {
        try {
            List<AssignmentService.TechnicianWithAssignments> techniciansWithAssignments = assignmentService
                    .getAllTechniciansWithAssignments();
            return ResponseEntity.ok(techniciansWithAssignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /// Get technician with assignments by ID
    @GetMapping("/manager/technicians/{id}/with-assignments")
    @ResponseBody
    public ResponseEntity<AssignmentService.TechnicianWithAssignments> getTechnicianWithAssignments(
            @PathVariable Long id, Authentication authentication) {
        try {
            AssignmentService.TechnicianWithAssignments technicianWithAssignments = assignmentService
                    .getTechnicianWithAssignments(id);
            if (technicianWithAssignments != null) {
                return ResponseEntity.ok(technicianWithAssignments);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /// Get technician workload statistics
    @GetMapping("/manager/technicians/{id}/workload-stats")
    @ResponseBody
    public ResponseEntity<AssignmentService.TechnicianWorkloadStats> getTechnicianWorkloadStats(@PathVariable Long id,
            Authentication authentication) {
        try {
            AssignmentService.TechnicianWorkloadStats stats = assignmentService.getTechnicianWorkloadStats(id);
            if (stats != null) {
                return ResponseEntity.ok(stats);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Update technician details
    @PostMapping("/manager/technicians/{id}/update-details")
    @ResponseBody
    public ResponseEntity<String> updateTechnicianDetails(@PathVariable Long id,
            @RequestParam String specialization,
            @RequestParam Integer maxDailyWorkload,
            @RequestParam BigDecimal hourlyRate,
            @RequestParam Integer experienceYears,
            Authentication authentication) {
        try {
            Technician updatedTechnician = assignmentService.updateTechnician(
                    id, specialization, maxDailyWorkload, hourlyRate, experienceYears);

            if (updatedTechnician != null) {
                return ResponseEntity.ok("Technician details updated successfully");
            } else {
                return ResponseEntity.badRequest().body("Technician not found");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating technician: " + e.getMessage());
        }
    }

    // Complete assignment
    @PostMapping("/manager/assignments/{id}/complete")
    @ResponseBody
    public ResponseEntity<String> completeAssignment(@PathVariable Long id, Authentication authentication) {
        try {
            assignmentService.completeAssignment(id);
            return ResponseEntity.ok("Assignment completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error completing assignment: " + e.getMessage());
        }
    }

    // Remove assignment

    @DeleteMapping("/manager/assignments/{id}")
    @ResponseBody
    public ResponseEntity<String> removeAssignment(@PathVariable Long id, Authentication authentication) {
        try {
            boolean removed = assignmentService.removeAssignment(id);
            if (removed) {
                return ResponseEntity.ok("Assignment removed successfully");
            } else {
                return ResponseEntity.badRequest().body("Assignment not found");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error removing assignment: " + e.getMessage());
        }
    }

    /// Update technician - Frontend compatible endpoint
    @PostMapping("/manager/technicians/{id}/update")
    @ResponseBody
    public ResponseEntity<String> updateTechnician(@PathVariable Long id,
            @RequestParam String specialization,
            @RequestParam Integer maxWorkload,
            @RequestParam(required = false) Double hourlyRate,
            @RequestParam(required = false) Integer experience,
            Authentication authentication) {
        try {
            BigDecimal hourlyRateBigDecimal = hourlyRate != null ? BigDecimal.valueOf(hourlyRate) : null;
            Technician updatedTechnician = assignmentService.updateTechnician(
                    id, specialization, maxWorkload, hourlyRateBigDecimal, experience);

            if (updatedTechnician != null) {
                return ResponseEntity.ok("Technician updated successfully");
            } else {
                return ResponseEntity.badRequest().body("Technician not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating technician: " + e.getMessage());
        }
    }

    /// Update assignment status
    @PostMapping("/manager/assignments/{id}/update-status")
    @ResponseBody
    public ResponseEntity<String> updateAssignmentStatus(@PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        try {
            TechnicianAssignment.AssignmentStatus newStatus = TechnicianAssignment.AssignmentStatus.valueOf(status);
            TechnicianAssignment updatedAssignment = assignmentService.updateAssignmentStatus(id, newStatus);

            if (updatedAssignment != null) {
                // Update notes if provided
                if (notes != null && !notes.trim().isEmpty()) {
                    updatedAssignment.setNotes(notes);
                    assignmentService.updateAssignment(updatedAssignment);
                }
                return ResponseEntity.ok("Assignment status updated successfully");
            } else {
                return ResponseEntity.badRequest().body("Assignment not found");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating assignment status: " + e.getMessage());
        }
    }

    /// View all feedbacks - Manager only
    @GetMapping("/manager/feedbacks")
    public String viewAllFeedbacks(Authentication authentication, Model model) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            // Check if user has manager role
            boolean isManager = currentUser.getRole() != null &&
                    "MANAGER".equals(currentUser.getRole().getRoleName());

            if (!isManager) {
                return "redirect:/login?error=Access denied - Manager role required";
            }

            List<Feedback> allFeedbacks = feedbackService.getAllFeedbacks();
            model.addAttribute("feedbacks", allFeedbacks);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("pageTitle", "All Customer Feedback");

            return "manager/feedback-list";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading feedbacks: " + e.getMessage());
            return "manager/feedback-list";
        }
    }

    /// View specific feedback details - Manager only
    @GetMapping("/manager/feedbacks/{id}")
    public String viewFeedbackDetails(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return "redirect:/login?error=User not found";
            }

            // Check if user has manager role
            boolean isManager = currentUser.getRole() != null &&
                    "MANAGER".equals(currentUser.getRole().getRoleName());

            if (!isManager) {
                return "redirect:/login?error=Access denied - Manager role required";
            }

            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);
            if (!feedbackOpt.isPresent()) {
                return "redirect:/manager/feedbacks?error=Feedback not found";
            }

            Feedback feedback = feedbackOpt.get();
            model.addAttribute("feedback", feedback);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("pageTitle", "Feedback Details");

            return "manager/feedback-details";
        } catch (Exception e) {
            return "redirect:/manager/feedbacks?error=Error loading feedback: " + e.getMessage();
        }
    }

    /// Delete feedback - Manager only
    @PostMapping("/manager/feedbacks/{id}/delete")
    public String deleteFeedback(@PathVariable Long id, Authentication authentication,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/manager/feedbacks";
            }

            // Check if user has manager role
            boolean isManager = currentUser.getRole() != null &&
                    "MANAGER".equals(currentUser.getRole().getRoleName());

            if (!isManager) {
                redirectAttributes.addFlashAttribute("error", "Access denied - Manager role required");
                return "redirect:/manager/feedbacks";
            }

            feedbackService.deleteFeedback(id);
            redirectAttributes.addFlashAttribute("message", "Feedback deleted successfully!");

            return "redirect:/manager/feedbacks";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting feedback: " + e.getMessage());
            return "redirect:/manager/feedbacks";
        }
    }

    // Get feedback statistics for manager dashboard

    @GetMapping("/manager/feedback-stats")
    @ResponseBody
    public ResponseEntity<?> getFeedbackStats(Authentication authentication) {
        try {
            // Get current user
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username).orElse(null);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Check if user has manager role
            boolean isManager = currentUser.getRole() != null &&
                    "MANAGER".equals(currentUser.getRole().getRoleName());

            if (!isManager) {
                return ResponseEntity.badRequest().body("Access denied - Manager role required");
            }

            List<Feedback> allFeedbacks = feedbackService.getAllFeedbacks();

            // Calculate statistics
            long totalFeedbacks = allFeedbacks.size();
            long excellentFeedbacks = allFeedbacks.stream()
                    .filter(f -> f.getRating() == 5)
                    .count();
            long goodFeedbacks = allFeedbacks.stream()
                    .filter(f -> f.getRating() == 4)
                    .count();
            long averageRating = totalFeedbacks > 0 ? (long) allFeedbacks.stream()
                    .mapToInt(Feedback::getRating)
                    .average()
                    .orElse(0.0) : 0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalFeedbacks", totalFeedbacks);
            stats.put("excellentFeedbacks", excellentFeedbacks);
            stats.put("goodFeedbacks", goodFeedbacks);
            stats.put("averageRating", averageRating);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching feedback stats: " + e.getMessage());
        }
    }

}