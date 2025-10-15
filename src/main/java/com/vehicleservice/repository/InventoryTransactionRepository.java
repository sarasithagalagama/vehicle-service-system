package com.vehicleservice.repository;

import com.vehicleservice.entity.InventoryTransaction;
import com.vehicleservice.entity.InventoryTransaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@org.springframework.context.annotation.Scope("singleton")
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByTransactionType(TransactionType transactionType);

    List<InventoryTransaction> findByItem_Id(Long itemId);

    List<InventoryTransaction> findByStaff_Id(Long staffId);

    List<InventoryTransaction> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t FROM InventoryTransaction t WHERE t.item.id = :itemId ORDER BY t.date DESC")
    List<InventoryTransaction> findByItemIdOrderByDateDesc(@Param("itemId") Long itemId);

    @Query("SELECT SUM(t.quantity) FROM InventoryTransaction t WHERE t.item.id = :itemId AND t.transactionType = :type")
    Integer getTotalQuantityByItemAndType(@Param("itemId") Long itemId, @Param("type") TransactionType type);
}
