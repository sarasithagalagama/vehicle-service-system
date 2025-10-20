package com.vehicleservice.service;

import com.vehicleservice.entity.*;
import com.vehicleservice.repository.TechnicianRepository;
import com.vehicleservice.repository.TechnicianAssignmentRepository;
import com.vehicleservice.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@org.springframework.context.annotation.Scope("singleton")
public class AssignmentService {

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private TechnicianAssignmentRepository assignmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // ==================== TECHNICIAN MANAGEMENT ====================

    /// Create a new technician with user account
    public Technician createTechnician(User user, String employeeId, String specialization,
            Integer maxDailyWorkload, BigDecimal hourlyRate, Integer experienceYears) {
        Technician technician = new Technician();
        technician.setUser(user);
        technician.setEmployeeId(employeeId);
        technician.setSpecialization(specialization);
        technician.setMaxDailyWorkload(maxDailyWorkload != null ? maxDailyWorkload : 6);
        technician.setCurrentWorkload(0);
        technician.setHourlyRate(hourlyRate);
        technician.setExperienceYears(experienceYears != null ? experienceYears : 0);
        technician.setIsActive(true);

        return technicianRepository.save(technician);
    }

    /// Get all active technicians
    public List<Technician> getAllActiveTechnicians() {
        return technicianRepository.findByIsActiveTrue();
    }

    /// Get the maximum employee ID for generating next ID
    public String getMaxEmployeeId() {
        return technicianRepository.findMaxEmployeeId();
    }

    /// Get technician by ID
    public Optional<Technician> getTechnicianById(Long id) {
        return technicianRepository.findById(id);
    }

    /// Get technician by user ID
    public Optional<Technician> getTechnicianByUserId(Long userId) {
        return technicianRepository.findByUser_Id(userId);
    }

    /// Update technician details
    public Technician updateTechnician(Long technicianId, String specialization,
            Integer maxDailyWorkload, BigDecimal hourlyRate, Integer experienceYears) {
        Optional<Technician> technicianOpt = technicianRepository.findById(technicianId);
        if (technicianOpt.isPresent()) {
            Technician technician = technicianOpt.get();
            technician.setSpecialization(specialization);
            technician.setMaxDailyWorkload(maxDailyWorkload);
            technician.setHourlyRate(hourlyRate);
            technician.setExperienceYears(experienceYears);
            technician.setUpdatedAt(LocalDateTime.now());
            return technicianRepository.save(technician);
        }
        return null;
    }

    /// Delete technician (soft delete)
    public boolean deleteTechnician(Long id) {
        Optional<Technician> technicianOpt = technicianRepository.findById(id);
        if (technicianOpt.isPresent()) {
            Technician technician = technicianOpt.get();
            technician.setIsActive(false);
            technician.setUpdatedAt(LocalDateTime.now());
            technicianRepository.save(technician);
            return true;
        }
        return false;
    }

    // ==================== ASSIGNMENT MANAGEMENT ====================

    /// Assign technician to booking
    public TechnicianAssignment assignTechnicianToBooking(Long bookingId, Long technicianId,
            User assignedBy, String notes) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        Optional<Technician> technicianOpt = technicianRepository.findById(technicianId);

        if (bookingOpt.isPresent() && technicianOpt.isPresent()) {
            // Check if assignment already exists
            Optional<TechnicianAssignment> existingAssignment = assignmentRepository
                    .findByBooking_IdAndTechnician_Id(bookingId, technicianId);

            if (existingAssignment.isPresent()) {
                throw new RuntimeException("Assignment already exists for this booking and technician");
            }

            // Create new assignment
            TechnicianAssignment assignment = new TechnicianAssignment();
            assignment.setBooking(bookingOpt.get());
            assignment.setTechnician(technicianOpt.get());
            assignment.setAssignedBy(assignedBy);
            assignment.setAssignmentDate(LocalDateTime.now());
            assignment.setStatus(TechnicianAssignment.AssignmentStatus.ASSIGNED);
            assignment.setNotes(notes);

            // Update technician workload
            Technician technician = technicianOpt.get();
            technician.setCurrentWorkload(technician.getCurrentWorkload() + 1);
            technician.setUpdatedAt(LocalDateTime.now());
            technicianRepository.save(technician);

            return assignmentRepository.save(assignment);
        }
        throw new RuntimeException("Booking or technician not found");
    }

    /// Assign technician to booking with specific assignment date
    public TechnicianAssignment assignTechnicianToBooking(Long bookingId, Long technicianId,
            User assignedBy, String notes, LocalDateTime assignmentDate) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        Optional<Technician> technicianOpt = technicianRepository.findById(technicianId);

        if (bookingOpt.isPresent() && technicianOpt.isPresent()) {
            // Check if assignment already exists
            Optional<TechnicianAssignment> existingAssignment = assignmentRepository
                    .findByBooking_IdAndTechnician_Id(bookingId, technicianId);

            if (existingAssignment.isPresent()) {
                throw new RuntimeException("Assignment already exists for this booking and technician");
            }

            // Create new assignment
            TechnicianAssignment assignment = new TechnicianAssignment();
            assignment.setBooking(bookingOpt.get());
            assignment.setTechnician(technicianOpt.get());
            assignment.setAssignedBy(assignedBy);
            assignment.setAssignmentDate(assignmentDate);
            assignment.setStatus(TechnicianAssignment.AssignmentStatus.ASSIGNED);
            assignment.setNotes(notes);

            // Update technician workload
            Technician technician = technicianOpt.get();
            technician.setCurrentWorkload(technician.getCurrentWorkload() + 1);
            technician.setUpdatedAt(LocalDateTime.now());
            technicianRepository.save(technician);

            return assignmentRepository.save(assignment);
        }
        throw new RuntimeException("Booking or technician not found");
    }

    /// Get all assignments
    public List<TechnicianAssignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    /// Get assignments by technician
    public List<TechnicianAssignment> getAssignmentsByTechnician(Long technicianId) {
        return assignmentRepository.findByTechnician_Id(technicianId);
    }

    /// Get assignments by booking
    public List<TechnicianAssignment> getAssignmentsByBooking(Long bookingId) {
        return assignmentRepository.findByBooking_Id(bookingId);
    }

    /// Get assignment by ID
    public Optional<TechnicianAssignment> getAssignmentById(Long id) {
        return assignmentRepository.findById(id);
    }

    /// Update assignment status
    public TechnicianAssignment updateAssignmentStatus(Long assignmentId,
            TechnicianAssignment.AssignmentStatus status) {
        Optional<TechnicianAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isPresent()) {
            TechnicianAssignment assignment = assignmentOpt.get();
            assignment.setStatus(status);
            assignment.setUpdatedAt(LocalDateTime.now());
            return assignmentRepository.save(assignment);
        }
        throw new RuntimeException("Assignment not found");
    }

    /// Update assignment
    public TechnicianAssignment updateAssignment(TechnicianAssignment assignment) {
        assignment.setUpdatedAt(LocalDateTime.now());
        return assignmentRepository.save(assignment);
    }

    /// Complete assignment and update workload
    public TechnicianAssignment completeAssignment(Long assignmentId) {
        Optional<TechnicianAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isPresent()) {
            TechnicianAssignment assignment = assignmentOpt.get();
            assignment.setStatus(TechnicianAssignment.AssignmentStatus.COMPLETED);
            assignment.setUpdatedAt(LocalDateTime.now());

            // Update technician workload
            Technician technician = assignment.getTechnician();
            technician.setCurrentWorkload(Math.max(0, technician.getCurrentWorkload() - 1));
            technician.setUpdatedAt(LocalDateTime.now());
            technicianRepository.save(technician);

            return assignmentRepository.save(assignment);
        }
        throw new RuntimeException("Assignment not found");
    }

    /// Remove assignment
    public boolean removeAssignment(Long assignmentId) {
        Optional<TechnicianAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isPresent()) {
            TechnicianAssignment assignment = assignmentOpt.get();

            // Update technician workload
            Technician technician = assignment.getTechnician();
            technician.setCurrentWorkload(Math.max(0, technician.getCurrentWorkload() - 1));
            technician.setUpdatedAt(LocalDateTime.now());
            technicianRepository.save(technician);

            assignmentRepository.deleteById(assignmentId);
            return true;
        }
        return false;
    }

    /// Delete assignments by booking
    public void deleteAssignmentsByBooking(Long bookingId) {
        List<TechnicianAssignment> assignments = assignmentRepository.findByBooking_Id(bookingId);
        for (TechnicianAssignment assignment : assignments) {
            // Update technician workload
            Technician technician = assignment.getTechnician();
            technician.setCurrentWorkload(Math.max(0, technician.getCurrentWorkload() - 1));
            technician.setUpdatedAt(LocalDateTime.now());
            technicianRepository.save(technician);
        }
        assignmentRepository.deleteByBooking_Id(bookingId);
    }

    // ==================== UNIFIED MANAGEMENT ====================

    /// Get technician with their assignments
    public TechnicianWithAssignments getTechnicianWithAssignments(Long technicianId) {
        Optional<Technician> technicianOpt = technicianRepository.findById(technicianId);
        if (technicianOpt.isPresent()) {
            Technician technician = technicianOpt.get();
            List<TechnicianAssignment> assignments = assignmentRepository.findByTechnician_Id(technicianId);
            return new TechnicianWithAssignments(technician, assignments);
        }
        return null;
    }

    /// Get all technicians with their assignments
    public List<TechnicianWithAssignments> getAllTechniciansWithAssignments() {
        List<Technician> technicians = technicianRepository.findByIsActiveTrue();
        return technicians.stream()
                .map(technician -> {
                    List<TechnicianAssignment> assignments = assignmentRepository
                            .findByTechnician_Id(technician.getId());
                    return new TechnicianWithAssignments(technician, assignments);
                })
                .collect(Collectors.toList());
    }

    /// Get workload statistics for technician
    public TechnicianWorkloadStats getTechnicianWorkloadStats(Long technicianId) {
        Optional<Technician> technicianOpt = technicianRepository.findById(technicianId);
        if (technicianOpt.isPresent()) {
            Technician technician = technicianOpt.get();
            List<TechnicianAssignment> assignments = assignmentRepository.findByTechnician_Id(technicianId);

            long assignedCount = assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.ASSIGNED)
                    .count();

            long inProgressCount = assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.IN_PROGRESS)
                    .count();

            long completedCount = assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.COMPLETED)
                    .count();

            long cancelledCount = assignments.stream()
                    .filter(a -> a.getStatus() == TechnicianAssignment.AssignmentStatus.CANCELLED)
                    .count();

            return new TechnicianWorkloadStats(technician, assignedCount, inProgressCount,
                    completedCount, cancelledCount);
        }
        return null;
    }

    /// Cleanup orphaned assignments
    public int cleanupOrphanedAssignments() {
        List<TechnicianAssignment> allAssignments = assignmentRepository.findAll();
        int cleanedCount = 0;

        for (TechnicianAssignment assignment : allAssignments) {
            try {
                // Try to access the booking to check if it exists
                assignment.getBooking().getId();
            } catch (Exception e) {
                // Delete assignment with missing booking
                assignmentRepository.deleteById(assignment.getId());
                cleanedCount++;
            }
        }
        return cleanedCount;
    }

    // ==================== INNER CLASSES ====================

    /// DTO for technician with assignments
    public static class TechnicianWithAssignments {
        private Technician technician;
        private List<TechnicianAssignment> assignments;

        public TechnicianWithAssignments(Technician technician, List<TechnicianAssignment> assignments) {
            this.technician = technician;
            this.assignments = assignments;
        }

        // Getters and setters
        public Technician getTechnician() {
            return technician;
        }

        public void setTechnician(Technician technician) {
            this.technician = technician;
        }

        public List<TechnicianAssignment> getAssignments() {
            return assignments;
        }

        public void setAssignments(List<TechnicianAssignment> assignments) {
            this.assignments = assignments;
        }
    }

    /// DTO for technician workload statistics
    public static class TechnicianWorkloadStats {
        private Technician technician;
        private long assignedCount;
        private long inProgressCount;
        private long completedCount;
        private long cancelledCount;

        public TechnicianWorkloadStats(Technician technician, long assignedCount, long inProgressCount,
                long completedCount, long cancelledCount) {
            this.technician = technician;
            this.assignedCount = assignedCount;
            this.inProgressCount = inProgressCount;
            this.completedCount = completedCount;
            this.cancelledCount = cancelledCount;
        }

        // Getters and setters
        public Technician getTechnician() {
            return technician;
        }

        public void setTechnician(Technician technician) {
            this.technician = technician;
        }

        public long getAssignedCount() {
            return assignedCount;
        }

        public void setAssignedCount(long assignedCount) {
            this.assignedCount = assignedCount;
        }

        public long getInProgressCount() {
            return inProgressCount;
        }

        public void setInProgressCount(long inProgressCount) {
            this.inProgressCount = inProgressCount;
        }

        public long getCompletedCount() {
            return completedCount;
        }

        public void setCompletedCount(long completedCount) {
            this.completedCount = completedCount;
        }

        public long getCancelledCount() {
            return cancelledCount;
        }

        public void setCancelledCount(long cancelledCount) {
            this.cancelledCount = cancelledCount;
        }
    }

}
