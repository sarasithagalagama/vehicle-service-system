package com.vehicleservice.service;

import com.vehicleservice.entity.InventoryItem;
import com.vehicleservice.entity.InventoryTransaction;
import com.vehicleservice.entity.InventoryTransaction.TransactionType;
import com.vehicleservice.entity.User;
import com.vehicleservice.repository.InventoryItemRepository;
import com.vehicleservice.repository.InventoryTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@org.springframework.context.annotation.Scope("singleton")
public class InventoryService {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    // Inventory Item methods
    public List<InventoryItem> getAllInventoryItems() {
        return inventoryItemRepository.findAll();
    }

    public Optional<InventoryItem> getInventoryItemById(Long id) {
        return inventoryItemRepository.findById(id);
    }

    public InventoryItem saveInventoryItem(InventoryItem item) {
        // Validate item before saving
        if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        if (item.getQuantity() == null || item.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (item.getUnitPrice() == null || item.getUnitPrice().doubleValue() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (item.getReorderLevel() == null || item.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }

        return inventoryItemRepository.save(item);
    }

    public InventoryItem updateInventoryItem(InventoryItem item) {
        // Check if item exists
        if (item.getId() == null || !inventoryItemRepository.existsById(item.getId())) {
            throw new IllegalArgumentException("Inventory item not found for update");
        }

        // Validate item before updating
        if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        if (item.getQuantity() == null || item.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (item.getUnitPrice() == null || item.getUnitPrice().doubleValue() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (item.getReorderLevel() == null || item.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }

        return inventoryItemRepository.save(item);
    }

    public void deleteInventoryItem(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Item ID cannot be null");
        }
        if (!inventoryItemRepository.existsById(id)) {
            throw new IllegalArgumentException("Inventory item not found for deletion");
        }
        inventoryItemRepository.deleteById(id);
    }

    public List<InventoryItem> searchInventoryItems(String keyword) {
        return inventoryItemRepository.findByKeyword(keyword);
    }

    public List<InventoryItem> getInventoryItemsByCategory(String category) {
        return inventoryItemRepository.findByCategory(category);
    }

    public List<InventoryItem> getItemsNeedingReorder() {
        return inventoryItemRepository.findItemsNeedingReorder();
    }

    public long getCountOfItemsNeedingReorder() {
        return inventoryItemRepository.countItemsNeedingReorder();
    }

    // Inventory Transaction methods
    public List<InventoryTransaction> getAllTransactions() {
        return inventoryTransactionRepository.findAll();
    }

    public Optional<InventoryTransaction> getTransactionById(Long id) {
        return inventoryTransactionRepository.findById(id);
    }

    public InventoryTransaction saveTransaction(InventoryTransaction transaction) {
        // Update inventory quantity based on transaction type
        InventoryItem item = transaction.getItem();
        if (transaction.getTransactionType() == TransactionType.IN) {
            item.setQuantity(item.getQuantity() + transaction.getQuantity());
        } else if (transaction.getTransactionType() == TransactionType.OUT) {
            if (item.getQuantity() >= transaction.getQuantity()) {
                item.setQuantity(item.getQuantity() - transaction.getQuantity());
            } else {
                throw new IllegalArgumentException("Insufficient inventory quantity");
            }
        }

        updateInventoryItem(item);
        return inventoryTransactionRepository.save(transaction);
    }

    public List<InventoryTransaction> getTransactionsByItem(Long itemId) {
        return inventoryTransactionRepository.findByItemIdOrderByDateDesc(itemId);
    }

    public List<InventoryTransaction> getTransactionsByStaff(Long staffId) {
        return inventoryTransactionRepository.findByStaff_Id(staffId);
    }

    public List<InventoryTransaction> getTransactionsByType(TransactionType type) {
        return inventoryTransactionRepository.findByTransactionType(type);
    }

    public List<InventoryTransaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return inventoryTransactionRepository.findByDateBetween(startDate, endDate);
    }

    public Integer getTotalQuantityByItemAndType(Long itemId, TransactionType type) {
        Integer total = inventoryTransactionRepository.getTotalQuantityByItemAndType(itemId, type);
        return total != null ? total : 0;
    }

    // Helper method to add stock
    public InventoryTransaction addStock(Long itemId, Integer quantity, User staff) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        InventoryTransaction transaction = new InventoryTransaction(
                item, TransactionType.IN, quantity, LocalDateTime.now(), staff);

        return saveTransaction(transaction);
    }

    // Helper method to remove stock
    public InventoryTransaction removeStock(Long itemId, Integer quantity, User staff) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        if (item.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient inventory quantity");
        }

        InventoryTransaction transaction = new InventoryTransaction(
                item, TransactionType.OUT, quantity, LocalDateTime.now(), staff);

        return saveTransaction(transaction);
    }

    public InventoryTransaction issueParts(Long itemId, Integer quantity, User technician, User issuedBy,
            String issueDate, String notes, String purpose) {
        InventoryItem item = getInventoryItemById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        // Check if sufficient stock is available
        if (item.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock! Available: " + item.getQuantity() + ", Requested: " + quantity);
        }

        // Create transaction record
        LocalDateTime transactionDate;
        try {
            // Handle datetime-local input format (YYYY-MM-DDTHH:mm)
            if (issueDate.contains("T")) {
                transactionDate = LocalDateTime.parse(issueDate);
            } else {
                // Fallback for other formats
                transactionDate = LocalDateTime.parse(issueDate + "T00:00");
            }
        } catch (Exception e) {
            transactionDate = LocalDateTime.now();
        }

        InventoryTransaction transaction = new InventoryTransaction(
                item, TransactionType.OUT, quantity, transactionDate, issuedBy);

        // Add additional details if needed (you might want to extend the entity)
        // For now, we'll use the basic transaction structure

        return saveTransaction(transaction);
    }
}
