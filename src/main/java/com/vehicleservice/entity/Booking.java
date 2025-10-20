package com.vehicleservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", unique = true, nullable = false, length = 50)
    private String bookingNumber;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "service_price", precision = 10, scale = 2)
    private BigDecimal servicePrice;

    @Column(name = "additional_charges", precision = 10, scale = 2)
    private BigDecimal additionalCharges;

    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "remaining_amount", precision = 10, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Booking() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.paymentStatus = PaymentStatus.PENDING;
        this.servicePrice = BigDecimal.ZERO;
        this.additionalCharges = BigDecimal.ZERO;
        this.totalPrice = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.remainingAmount = BigDecimal.ZERO;
    }

    public Booking(String bookingNumber, String customerName, String vehicleNumber,
            String serviceType, LocalDateTime bookingDate, String notes) {
        this();
        this.bookingNumber = bookingNumber;
        this.customerName = customerName;
        this.vehicleNumber = vehicleNumber;
        this.serviceType = serviceType;
        this.bookingDate = bookingDate;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
        this.updatedAt = LocalDateTime.now();
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
        this.updatedAt = LocalDateTime.now();
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getServicePrice() {
        return servicePrice;
    }

    public void setServicePrice(BigDecimal servicePrice) {
        this.servicePrice = servicePrice;
        this.updatedAt = LocalDateTime.now();
        calculateTotalPrice();
    }

    public BigDecimal getAdditionalCharges() {
        return additionalCharges;
    }

    public void setAdditionalCharges(BigDecimal additionalCharges) {
        this.additionalCharges = additionalCharges;
        this.updatedAt = LocalDateTime.now();
        calculateTotalPrice();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
        this.updatedAt = LocalDateTime.now();
        // Auto-calculate remaining amount
        if (this.totalPrice != null && paidAmount != null) {
            this.remainingAmount = this.totalPrice.subtract(paidAmount);
        }
        // Auto-update payment status based on amounts
        updatePaymentStatus();
    }

    /// Update payment status based on paid amount and total price
    private void updatePaymentStatus() {
        if (this.totalPrice == null || this.paidAmount == null) {
            return;
        }

        double totalPrice = this.totalPrice.doubleValue();
        double paidAmount = this.paidAmount.doubleValue();

        if (paidAmount >= totalPrice) {
            this.paymentStatus = PaymentStatus.PAID;
        } else if (paidAmount > 0) {
            this.paymentStatus = PaymentStatus.PARTIAL;
        } else {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        this.updatedAt = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper method to calculate total price
    public void calculateTotalPrice() {
        BigDecimal service = this.servicePrice != null ? this.servicePrice : BigDecimal.ZERO;
        BigDecimal additional = this.additionalCharges != null ? this.additionalCharges : BigDecimal.ZERO;
        this.totalPrice = service.add(additional);
    }

    // Enum for payment status
    public enum PaymentStatus {
        PENDING, PAID, PARTIAL, REFUNDED
    }
}
