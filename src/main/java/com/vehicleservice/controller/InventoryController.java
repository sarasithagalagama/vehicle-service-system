package com.vehicleservice.controller;

import com.vehicleservice.entity.InventoryItem;
import com.vehicleservice.entity.InventoryTransaction;
import com.vehicleservice.entity.User;
import com.vehicleservice.service.InventoryService;
import com.vehicleservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/staff/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserService userService;

    // Save new inventory item
    @PostMapping("/save")
    public String saveInventoryItem(@ModelAttribute InventoryItem item, 
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            // Enhanced validation
            if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Item name is required and cannot be empty.");
                return "redirect:/staff/dashboard";
            }
            
            if (item.getItemName().length() < 2 || item.getItemName().length() > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Item name must be between 2 and 100 characters.");
                return "redirect:/staff/dashboard";
            }
            
            if (!item.getItemName().matches("^[A-Za-z0-9\\s\\-_]+$")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Item name can only contain letters, numbers, spaces, hyphens, and underscores.");
                return "redirect:/staff/dashboard";
            }
            
            if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Category is required and cannot be empty.");
                return "redirect:/staff/dashboard";
            }
            
            if (item.getQuantity() == null || item.getQuantity() < 1 || item.getQuantity() > 50) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Quantity must be between 1 and 50.");
                return "redirect:/staff/dashboard";
            }
            
            if (item.getReorderLevel() == null || item.getReorderLevel() < 1 || item.getReorderLevel() > 50) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Reorder level must be between 1 and 50.");
                return "redirect:/staff/dashboard";
            }
            
            if (item.getUnitPrice() == null || item.getUnitPrice().doubleValue() < 0 || item.getUnitPrice().doubleValue() > 99999.99) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Unit price must be between 0.00 and 99,999.99.");
                return "redirect:/staff/dashboard";
            }
            
            // Set timestamps
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            
            // Save the item
            inventoryService.saveInventoryItem(item);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Inventory item '" + item.getItemName() + "' has been added successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error adding inventory item: " + e.getMessage());
        }
        
        return "redirect:/staff/dashboard";
    }

    // Update existing inventory item
    @PostMapping("/update")
    public String updateInventoryItem(@ModelAttribute InventoryItem item,
                                    @RequestParam(value = "redirect", defaultValue = "/dashboard") String redirectUrl,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Enhanced validation
            if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Item name is required and cannot be empty.");
                return "redirect:" + redirectUrl;
            }
            
            if (item.getItemName().length() < 2 || item.getItemName().length() > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Item name must be between 2 and 100 characters.");
                return "redirect:" + redirectUrl;
            }
            
            if (!item.getItemName().matches("^[A-Za-z0-9\\s\\-_]+$")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Item name can only contain letters, numbers, spaces, hyphens, and underscores.");
                return "redirect:" + redirectUrl;
            }
            
            if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Category is required and cannot be empty.");
                return "redirect:" + redirectUrl;
            }
            
            if (item.getQuantity() == null || item.getQuantity() < 1 || item.getQuantity() > 50) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Quantity must be between 1 and 50.");
                return "redirect:" + redirectUrl;
            }
            
            if (item.getReorderLevel() == null || item.getReorderLevel() < 1 || item.getReorderLevel() > 50) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Reorder level must be between 1 and 50.");
                return "redirect:" + redirectUrl;
            }
            
            if (item.getUnitPrice() == null || item.getUnitPrice().doubleValue() < 0 || item.getUnitPrice().doubleValue() > 99999.99) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Unit price must be between 0.00 and 99,999.99.");
                return "redirect:" + redirectUrl;
            }
            
            // Get existing item to preserve timestamps
            Optional<InventoryItem> existingItem = inventoryService.getInventoryItemById(item.getId());
            if (existingItem.isPresent()) {
                InventoryItem existing = existingItem.get();
                item.setCreatedAt(existing.getCreatedAt());
                item.setUpdatedAt(LocalDateTime.now());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Inventory item not found for update.");
                return "redirect:" + redirectUrl;
            }
            
            // Update the item
            inventoryService.updateInventoryItem(item);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Inventory item '" + item.getItemName() + "' has been updated successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating inventory item: " + e.getMessage());
        }
        
        return "redirect:" + redirectUrl;
    }

    // Delete inventory item
    @GetMapping("/delete/{id}")
    public String deleteInventoryItem(@PathVariable Long id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            Optional<InventoryItem> item = inventoryService.getInventoryItemById(id);
            String itemName = item.isPresent() ? item.get().getItemName() : "Unknown";
            
            inventoryService.deleteInventoryItem(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Inventory item '" + itemName + "' has been deleted successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error deleting inventory item: " + e.getMessage());
        }
        
        return "redirect:/staff/dashboard";
    }

    // Add stock to inventory item
    @PostMapping("/add-stock/{id}")
    public String addStock(@PathVariable Long id,
                          @RequestParam Integer quantity,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            // Enhanced validation
            if (quantity == null || quantity <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Quantity must be a positive number.");
                return "redirect:/staff/dashboard";
            }
            
            if (quantity > 50) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Quantity cannot exceed 50 units.");
                return "redirect:/staff/dashboard";
            }
            
            // Check if item exists
            Optional<InventoryItem> item = inventoryService.getInventoryItemById(id);
            if (!item.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Inventory item not found.");
                return "redirect:/staff/dashboard";
            }
            
            User currentUser = (User) authentication.getPrincipal();
            
            // Add stock using the service
            inventoryService.addStock(id, quantity, currentUser);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Successfully added " + quantity + " units to " + item.get().getItemName() + "!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error adding stock: " + e.getMessage());
        }
        
        return "redirect:/staff/dashboard";
    }


    // Issue parts to technicians
    @PostMapping("/issue-parts")
    public String issueParts(@RequestParam Long technicianId,
                           @RequestParam Long itemId,
                           @RequestParam Integer quantity,
                           @RequestParam String issueDate,
                           @RequestParam(required = false) String notes,
                           @RequestParam(required = false) String purpose,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            // Enhanced validation
            if (technicianId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Please select a technician.");
                return "redirect:/staff/dashboard";
            }
            
            if (itemId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Please select an item.");
                return "redirect:/staff/dashboard";
            }
            
            if (quantity == null || quantity <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Quantity must be a positive number.");
                return "redirect:/staff/dashboard";
            }
            
            if (quantity > 50) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Quantity cannot exceed 50 units.");
                return "redirect:/staff/dashboard";
            }
            
            if (issueDate == null || issueDate.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Please select an issue date.");
                return "redirect:/staff/dashboard";
            }
            
            // Validate date format and not in past
            try {
                LocalDateTime selectedDate = LocalDateTime.parse(issueDate);
                LocalDateTime now = LocalDateTime.now();
                
                // Allow current time but not past times
                if (selectedDate.isBefore(now.minusMinutes(1))) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Issue date cannot be in the past. Please select today's date or a future date.");
                    return "redirect:/staff/dashboard";
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Invalid date format. Please use the date picker to select a valid date.");
                return "redirect:/staff/dashboard";
            }
            
            User currentUser = (User) authentication.getPrincipal();
            User technician = userService.getUserById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found"));
            
            // Check if item exists and has sufficient stock
            Optional<InventoryItem> item = inventoryService.getInventoryItemById(itemId);
            if (!item.isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Inventory item not found.");
                return "redirect:/staff/dashboard";
            }
            
            if (item.get().getQuantity() < quantity) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Insufficient stock! Available: " + item.get().getQuantity() + ", Requested: " + quantity);
                return "redirect:/staff/dashboard";
            }
            
            // Issue parts using the service
            inventoryService.issueParts(itemId, quantity, technician, currentUser, 
                                      issueDate, notes, purpose);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Successfully issued " + quantity + " " + item.get().getItemName() + " to " + 
                (technician.getFirstName() != null ? technician.getFirstName() + " " + technician.getLastName() : technician.getUsername()) + "!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error issuing parts: " + e.getMessage());
        }
        
        return "redirect:/staff/dashboard";
    }

    // Get inventory item details for AJAX requests
    @GetMapping("/item/{id}")
    @ResponseBody
    public InventoryItem getInventoryItem(@PathVariable Long id) {
        return inventoryService.getInventoryItemById(id).orElse(null);
    }

    // Get low stock items for AJAX requests
    @GetMapping("/low-stock")
    @ResponseBody
    public List<InventoryItem> getLowStockItems() {
        return inventoryService.getItemsNeedingReorder();
    }

    // Get recent transactions for AJAX requests
    @GetMapping("/transactions")
    @ResponseBody
    public List<InventoryTransaction> getRecentTransactions(@RequestParam(defaultValue = "10") int limit) {
        List<InventoryTransaction> allTransactions = inventoryService.getAllTransactions();
        return allTransactions.stream()
            .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
}
