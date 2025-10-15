package com.vehicleservice.repository;

import com.vehicleservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@org.springframework.context.annotation.Scope("singleton")
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByCategory(String category);

    List<InventoryItem> findByItemNameContainingIgnoreCase(String itemName);

    @Query("SELECT i FROM InventoryItem i WHERE i.quantity <= i.reorderLevel")
    List<InventoryItem> findItemsNeedingReorder();

    @Query("SELECT i FROM InventoryItem i WHERE " +
            "i.itemName LIKE %:keyword% OR " +
            "i.category LIKE %:keyword%")
    List<InventoryItem> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.quantity <= i.reorderLevel")
    long countItemsNeedingReorder();

    Optional<InventoryItem> findByItemName(String itemName);
}
