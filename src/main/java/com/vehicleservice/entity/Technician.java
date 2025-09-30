package com.vehicleservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "technicians")
public class Technician extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(name = "employee_id", unique = true, nullable = false)
    private String employeeId;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "max_daily_workload", nullable = false)
    private Integer maxDailyWorkload = 6;

    @Column(name = "current_workload", nullable = false)
    private Integer currentWorkload = 0;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "experience_years")
    private Integer experienceYears = 0;

    // Common fields (createdAt, updatedAt, isActive) are now inherited from BaseEntity

    // Constructors
    public Technician() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Technician(User user, String employeeId) {
        this();
        this.user = user;
        this.employeeId = employeeId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getMaxDailyWorkload() {
        return maxDailyWorkload;
    }

    public void setMaxDailyWorkload(Integer maxDailyWorkload) {
        this.maxDailyWorkload = maxDailyWorkload;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getCurrentWorkload() {
        return currentWorkload;
    }

    public void setCurrentWorkload(Integer currentWorkload) {
        this.currentWorkload = currentWorkload;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
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

    // Helper methods
    public boolean canTakeMoreWork() {
        return isActive && 
               currentWorkload != null && 
               maxDailyWorkload != null && 
               currentWorkload < maxDailyWorkload;
    }

    public void addWorkload() {
        if (currentWorkload != null && maxDailyWorkload != null) {
            this.currentWorkload = Math.min(currentWorkload + 1, maxDailyWorkload);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeWorkload() {
        if (currentWorkload != null) {
            this.currentWorkload = Math.max(currentWorkload - 1, 0);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public int getRemainingCapacity() {
        if (maxDailyWorkload != null && currentWorkload != null) {
            return Math.max(0, maxDailyWorkload - currentWorkload);
        }
        return 0;
    }

    public String getFullName() {
        return user != null ? user.getFullName() : "Unknown";
    }
    
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
}
