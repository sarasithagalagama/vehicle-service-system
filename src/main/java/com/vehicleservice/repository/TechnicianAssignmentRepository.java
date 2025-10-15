package com.vehicleservice.repository;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.Technician;
import com.vehicleservice.entity.TechnicianAssignment;
import com.vehicleservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@org.springframework.context.annotation.Scope("singleton")
public interface TechnicianAssignmentRepository extends JpaRepository<TechnicianAssignment, Long> {

    // Find assignments by technician
    List<TechnicianAssignment> findByTechnicianOrderByAssignmentDateDesc(Technician technician);

    // Find assignments by booking
    List<TechnicianAssignment> findByBookingOrderByAssignmentDateDesc(Booking booking);

    // Find assignments by assigned by user
    List<TechnicianAssignment> findByAssignedByOrderByAssignmentDateDesc(User assignedBy);

    // Find assignments by status
    List<TechnicianAssignment> findByStatusOrderByAssignmentDateDesc(TechnicianAssignment.AssignmentStatus status);

    // Find assignments for a date range
    List<TechnicianAssignment> findByAssignmentDateBetweenOrderByAssignmentDateDesc(LocalDateTime startDate,
            LocalDateTime endDate);

    // Find active assignments for a technician
    @Query("SELECT ta FROM TechnicianAssignment ta WHERE ta.technician = :technician AND ta.status IN ('ASSIGNED', 'IN_PROGRESS') ORDER BY ta.assignmentDate DESC")
    List<TechnicianAssignment> findActiveAssignmentsByTechnician(@Param("technician") Technician technician);

    // Find assignments by booking and technician
    Optional<TechnicianAssignment> findByBookingAndTechnician(Booking booking, Technician technician);

    // Count assignments by technician and status
    @Query("SELECT COUNT(ta) FROM TechnicianAssignment ta WHERE ta.technician = :technician AND ta.status = :status")
    long countByTechnicianAndStatus(@Param("technician") Technician technician,
            @Param("status") TechnicianAssignment.AssignmentStatus status);

    // Find assignments by technician and date range
    @Query("SELECT ta FROM TechnicianAssignment ta WHERE ta.technician = :technician AND ta.assignmentDate >= :startDate AND ta.assignmentDate <= :endDate ORDER BY ta.assignmentDate DESC")
    List<TechnicianAssignment> findByTechnicianAndDateRange(@Param("technician") Technician technician,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Delete assignments by booking
    void deleteByBooking(Booking booking);

    // Delete assignments by technician
    void deleteByTechnician(Technician technician);

    // Find assignments by technician ID
    List<TechnicianAssignment> findByTechnician_Id(Long technicianId);

    // Find assignments by booking ID
    List<TechnicianAssignment> findByBooking_Id(Long bookingId);

    // Find assignment by booking ID and technician ID
    Optional<TechnicianAssignment> findByBooking_IdAndTechnician_Id(Long bookingId, Long technicianId);

    // Delete assignments by booking ID
    void deleteByBooking_Id(Long bookingId);
}
