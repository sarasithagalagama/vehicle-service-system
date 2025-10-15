package com.vehicleservice.repository;

import com.vehicleservice.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@org.springframework.stereotype.Repository
@org.springframework.context.annotation.Scope("singleton")
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Find all active feedbacks
    @Query("SELECT f FROM Feedback f WHERE f.isActive = true")
    List<Feedback> findAllActive();

    // Find feedbacks by booking ID (active only)
    @Query("SELECT f FROM Feedback f WHERE f.booking.id = :bookingId AND f.isActive = true")
    List<Feedback> findByBookingId(@Param("bookingId") Long bookingId);

    // Find feedbacks by user ID (active only)
    @Query("SELECT f FROM Feedback f WHERE f.user.id = :userId AND f.isActive = true")
    List<Feedback> findByUserId(@Param("userId") Long userId);

    // Check if active feedback exists for a booking
    @Query("SELECT COUNT(f) > 0 FROM Feedback f WHERE f.booking.id = :bookingId AND f.isActive = true")
    boolean existsByBookingId(@Param("bookingId") Long bookingId);

    // Find all feedbacks (including inactive) - for admin purposes
    @Query("SELECT f FROM Feedback f")
    List<Feedback> findAllIncludingInactive();
}
