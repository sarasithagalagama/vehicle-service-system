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
@org.springframework.context.annotation.Scope("singleton")
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

    // Payment method queries
    List<Booking> findByPaymentMethod(String paymentMethod);

    List<Booking> findByPaymentMethodAndPaymentStatus(String paymentMethod, PaymentStatus paymentStatus);

    @Query("SELECT b FROM Booking b WHERE b.paymentMethod IS NOT NULL")
    List<Booking> findBookingsWithPaymentMethod();

    @Query("SELECT DISTINCT b.paymentMethod FROM Booking b WHERE b.paymentMethod IS NOT NULL")
    List<String> findDistinctPaymentMethods();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.paymentMethod = :paymentMethod")
    Long countByPaymentMethod(@Param("paymentMethod") String paymentMethod);

    // Check if booking number exists
    boolean existsByBookingNumber(String bookingNumber);

    // Find booking by booking number
    java.util.Optional<Booking> findByBookingNumber(String bookingNumber);

}
