package com.vehicleservice.repository;

import com.vehicleservice.entity.Technician;
import com.vehicleservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@org.springframework.context.annotation.Scope("singleton")
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    // Find technician by user
    Optional<Technician> findByUser(User user);

    // Find technician by user ID
    Optional<Technician> findByUser_Id(Long userId);

    // Find technician by employee ID
    Optional<Technician> findByEmployeeId(String employeeId);

    // Find active technicians
    List<Technician> findByIsActiveTrue();

    // Find technicians by specialization
    List<Technician> findBySpecializationAndIsActiveTrue(String specialization);

    // Find technicians who can take more work
    @Query("SELECT t FROM Technician t WHERE t.isActive = true AND t.currentWorkload < t.maxDailyWorkload")
    List<Technician> findAvailableTechnicians();

    // Find technicians by specialization who can take more work
    @Query("SELECT t FROM Technician t WHERE t.isActive = true AND t.specialization = :specialization AND t.currentWorkload < t.maxDailyWorkload")
    List<Technician> findAvailableTechniciansBySpecialization(@Param("specialization") String specialization);

    // Find technicians with workload information
    @Query("SELECT t FROM Technician t WHERE t.isActive = true ORDER BY t.currentWorkload ASC, t.maxDailyWorkload DESC")
    List<Technician> findTechniciansOrderedByWorkload();

    // Count technicians by specialization
    @Query("SELECT t.specialization, COUNT(t) FROM Technician t WHERE t.isActive = true GROUP BY t.specialization")
    List<Object[]> countTechniciansBySpecialization();

    // Find technicians with specific workload range
    @Query("SELECT t FROM Technician t WHERE t.isActive = true AND t.currentWorkload BETWEEN :minWorkload AND :maxWorkload")
    List<Technician> findTechniciansByWorkloadRange(@Param("minWorkload") Integer minWorkload,
            @Param("maxWorkload") Integer maxWorkload);

    // Find the maximum employee ID for generating next ID
    @Query("SELECT MAX(t.employeeId) FROM Technician t")
    String findMaxEmployeeId();
}
