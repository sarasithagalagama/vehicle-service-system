package com.vehicleservice.repository;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.Booking.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPaymentStatus(PaymentStatus paymentStatus);
    List<Booking> findByCustomerNameContainingIgnoreCase(String customerName);
    List<Booking> findByVehicleNumberContainingIgnoreCase(String vehicleNumber);
    List<Booking> findByBookingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE " +
           "b.customerName LIKE %:keyword% OR " +
           "b.vehicleNumber LIKE %:keyword% OR " +
           "b.serviceType LIKE %:keyword% OR " +
           "b.bookingNumber LIKE %:keyword%")
    List<Booking> findByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT b FROM Booking b WHERE CAST(b.bookingDate AS date) = :bookingDate")
    List<Booking> findByBookingDate(@Param("bookingDate") LocalDate bookingDate);
    
}
