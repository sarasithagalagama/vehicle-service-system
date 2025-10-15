package com.vehicleservice.controller;

// Import statements for required classes and annotations
import com.vehicleservice.entity.Role;
import com.vehicleservice.entity.User;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Controller class for admin functionality
@Controller
@RequestMapping("/admin")
@org.springframework.context.annotation.Scope("singleton")
public class AdminController {

    // Service dependencies for user management and assignments
    @Autowired
    private UserService userService;

    @Autowired
    private AssignmentService technicianAssignmentService;

    // Display admin dashboard with user management functionality
    @GetMapping("/dashboard")
    public String adminDashboard(Authentication authentication, Model model,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "desc") String direction) {

        User currentUser = (User) authentication.getPrincipal();
        if (!currentUser.getRole().getRoleName().equals("ADMIN")) {
            return "redirect:/dashboard";
        }

        // Get user statistics
        long totalUsers = userService.getUserCount();
        long adminUsers = userService.getUserCountByRole("ADMIN");
        long managerUsers = userService.getUserCountByRole("MANAGER");
        long receptionistUsers = userService.getUserCountByRole("RECEPTIONIST");
        long technicianUsers = userService.getUserCountByRole("TECHNICIAN");
        long inventoryManagerUsers = userService.getUserCountByRole("INVENTORY_MANAGER");
        long customerUsers = userService.getUserCountByRole("CUSTOMER");

        long staffUsers = managerUsers + receptionistUsers + technicianUsers + inventoryManagerUsers;

        // Get paginated users with filters
        Page<User> usersPage = userService.getUsersWithFilters(search, role, status, page, size);

        // Get all roles for forms
        List<Role> roles = userService.getAllRoles();

        // Add attributes to model
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("adminUsers", adminUsers);
        model.addAttribute("managerUsers", managerUsers);
        model.addAttribute("receptionistUsers", receptionistUsers);
        model.addAttribute("technicianUsers", technicianUsers);
        model.addAttribute("inventoryManagerUsers", inventoryManagerUsers);
        model.addAttribute("staffUsers", staffUsers);
        model.addAttribute("customerUsers", customerUsers);

        model.addAttribute("allUsers", usersPage.getContent());
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalElements", usersPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("pageSize", size);
        model.addAttribute("roles", roles);

        // Pagination attributes
        model.addAttribute("hasPrevious", usersPage.hasPrevious());
        model.addAttribute("hasNext", usersPage.hasNext());

        // Filter attributes
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "admin/dashboard";
    }

    // Create new user with specified details
    @PostMapping("/users")
    public String createUser(@RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Long roleId,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(defaultValue = "true") String isActive,
            RedirectAttributes redirectAttributes) {
        User user = null;
        try {
            // Create User object manually
            user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setPhoneNumber(phoneNumber);
            user.setAddress(address);
            user.setCity(city);
            user.setState(state);
            user.setZipCode(zipCode);
            user.setIsActive("true".equalsIgnoreCase(isActive));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            // Set date of birth if provided
            if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                try {
                    user.setDateOfBirth(LocalDateTime.parse(dateOfBirth + "T00:00:00"));
                } catch (Exception e) {
                    // If parsing fails, leave dateOfBirth as null
                }
            }

            // Server-side validation
            String validationError = validateUserInput(user, phoneNumber, dateOfBirth, address, city, state, zipCode);
            if (validationError != null) {
                redirectAttributes.addFlashAttribute("error", validationError);
                return "redirect:/admin/dashboard";
            }

            // Check for existing username
            if (userService.getUserByUsername(user.getUsername()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/admin/dashboard";
            }

            // Check for existing email
            if (userService.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/dashboard";
            }

            // Check for existing phone number (if provided)
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                if (userService.existsByPhoneNumber(phoneNumber)) {
                    redirectAttributes.addFlashAttribute("error", "Phone number already exists!");
                    return "redirect:/admin/dashboard";
                }
            }

            // Fields are already set above

            // Set role
            if (roleId == null) {
                redirectAttributes.addFlashAttribute("error", "Role is required!");
                return "redirect:/admin/dashboard";
            }

            try {
                Role role = userService.getRoleById(roleId);
                user.setRole(role);
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid role selected! Role ID: " + roleId);
                return "redirect:/admin/dashboard";
            }

            try {
                User savedUser = userService.saveUser(user);

                // If user has TECHNICIAN role, automatically create Technician record
                if (savedUser.getRole().getRoleName().equals("TECHNICIAN")) {
                    try {
                        // Generate unique employee ID
                        String employeeId = generateUniqueEmployeeId();

                        // Create technician record with default values
                        technicianAssignmentService.createTechnician(
                                savedUser,
                                employeeId,
                                "General Repair & Maintenance",
                                6, // max daily workload
                                new java.math.BigDecimal("1500.00"), // hourly rate
                                2 // experience years
                        );
                        redirectAttributes.addFlashAttribute("message", "User and Technician created successfully!");
                    } catch (Exception techException) {
                        redirectAttributes.addFlashAttribute("message",
                                "User created but failed to create technician record: " + techException.getMessage());
                    }
                } else {
                    redirectAttributes.addFlashAttribute("message", "User created successfully!");
                }
            } catch (Exception saveException) {
                throw saveException; // Re-throw to be caught by outer catch block
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create user: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    // Validation helper method
    private String validateUserInput(User user, String phoneNumber, String dateOfBirth,
            String address, String city, String state, String zipCode) {
        // Validate first name
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            return "First name is required";
        }
        if (!user.getFirstName().matches("^[A-Za-z\\s'-]{2,50}$")) {
            return "First name must be 2-50 characters, letters, spaces, hyphens, and apostrophes only";
        }

        // Validate last name
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            return "Last name is required";
        }
        if (!user.getLastName().matches("^[A-Za-z\\s'-]{2,50}$")) {
            return "Last name must be 2-50 characters, letters, spaces, hyphens, and apostrophes only";
        }

        // Validate username
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return "Username is required";
        }
        if (!user.getUsername().matches("^[A-Za-z0-9_]{3,20}$")) {
            return "Username must be 3-20 characters, letters, numbers, and underscores only";
        }

        // Validate email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return "Email is required";
        }
        if (!user.getEmail().matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$")) {
            return "Please enter a valid email address";
        }

        // Validate password (only for new users)
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (!user.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$")) {
                return "Password must be at least 8 characters with uppercase, lowercase, and number";
            }
        }

        // Validate phone number (optional)
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            // For Sri Lankan numbers, expect 9 digits
            String cleanPhone = phoneNumber.replaceAll("[^\\d]", "");
            if (!cleanPhone.matches("^[0-9]{9}$")) {
                return "Please enter a valid 9-digit Sri Lankan phone number";
            }
        }

        // Validate date of birth (optional)
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                LocalDateTime dob = LocalDateTime.parse(dateOfBirth + "T00:00:00");
                if (dob.isAfter(LocalDateTime.of(2010, 12, 31, 23, 59, 59))) {
                    return "Date of birth must be before 2011";
                }
            } catch (Exception e) {
                return "Please enter a valid date of birth";
            }
        }

        // Validate address (optional) - more lenient
        if (address != null && !address.trim().isEmpty()) {
            String trimmedAddress = address.trim();
            if (trimmedAddress.length() < 5 || trimmedAddress.length() > 200) {
                return "Address must be 5-200 characters";
            }
            // Allow letters, numbers, spaces, commas, periods, slashes, and hyphens
            if (!trimmedAddress.matches("^[A-Za-z0-9\\s,./-]+$")) {
                return "Address can only contain letters, numbers, spaces, commas, periods, slashes, and hyphens";
            }
        }

        // Validate city (optional) - more lenient
        if (city != null && !city.trim().isEmpty()) {
            String trimmedCity = city.trim();
            if (trimmedCity.length() < 2 || trimmedCity.length() > 50) {
                return "City must be 2-50 characters";
            }
        }

        // Validate state (optional) - more lenient
        if (state != null && !state.trim().isEmpty()) {
            String trimmedState = state.trim();
            if (trimmedState.length() < 2 || trimmedState.length() > 50) {
                return "State must be 2-50 characters";
            }
        }

        // Validate zip code (optional) - more lenient
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            String trimmedZip = zipCode.trim();
            if (trimmedZip.length() < 3 || trimmedZip.length() > 10) {
                return "ZIP code must be 3-10 characters";
            }
        }

        return null; // No validation errors
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam Long roleId,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String zipCode,
            @RequestParam(required = false) String dateOfBirth,
            @RequestParam(defaultValue = "true") String isActive,
            RedirectAttributes redirectAttributes) {
        try {
            User existingUser = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create User object manually for validation
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setPhoneNumber(phoneNumber);
            user.setAddress(address);
            user.setCity(city);
            user.setState(state);
            user.setZipCode(zipCode);
            user.setIsActive("true".equalsIgnoreCase(isActive));

            // Set date of birth if provided
            if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                try {
                    user.setDateOfBirth(LocalDateTime.parse(dateOfBirth + "T00:00:00"));
                } catch (Exception e) {
                    // If parsing fails, leave dateOfBirth as null
                }
            }

            // Server-side validation
            String validationError = validateUserInput(user, phoneNumber, dateOfBirth, address, city, state, zipCode);
            if (validationError != null) {
                redirectAttributes.addFlashAttribute("error", validationError);
                return "redirect:/admin/dashboard";
            }

            // Check for username conflicts (excluding current user)
            if (!existingUser.getUsername().equals(user.getUsername()) &&
                    userService.getUserByUsername(user.getUsername()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/admin/dashboard";
            }

            // Check for email conflicts (excluding current user)
            if (!existingUser.getEmail().equals(user.getEmail()) &&
                    userService.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/dashboard";
            }

            // Update fields
            existingUser.setUsername(user.getUsername());
            existingUser.setEmail(user.getEmail());
            existingUser.setFirstName(user.getFirstName());
            existingUser.setLastName(user.getLastName());
            existingUser.setPhoneNumber(phoneNumber);
            existingUser.setAddress(address);
            existingUser.setCity(city);
            existingUser.setState(state);
            existingUser.setZipCode(zipCode);
            existingUser.setIsActive("true".equalsIgnoreCase(isActive));
            existingUser.setUpdatedAt(LocalDateTime.now());

            // Update password only if provided and not empty
            if (password != null && !password.trim().isEmpty()) {
                existingUser.setPassword(password);
            }
            // If password is not provided, don't change the existing password

            // Update date of birth if provided
            if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
                try {
                    existingUser.setDateOfBirth(LocalDateTime.parse(dateOfBirth + "T00:00:00"));
                } catch (Exception e) {
                    // If parsing fails, leave dateOfBirth as is
                }
            }

            // Update role
            Role role = userService.getRoleById(roleId);
            existingUser.setRole(role);

            userService.updateUser(existingUser);
            redirectAttributes.addFlashAttribute("message", "User updated successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            // Prevent users from deleting themselves
            if (currentUser.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "You cannot delete your own account!");
                return "redirect:/admin/dashboard";
            }

            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "User deleted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Prevent users from deactivating themselves
            if (currentUser.getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot deactivate your own account!");
                return "redirect:/admin/dashboard";
            }

            user.setIsActive(!user.getIsActive());
            user.setUpdatedAt(LocalDateTime.now());
            userService.updateUser(user);

            String status = user.getIsActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("message", "User " + status + " successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user status: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    // Helper method to generate unique employee ID
    private String generateUniqueEmployeeId() {
        try {
            // Get the highest existing employee ID number
            String maxEmployeeId = technicianAssignmentService.getMaxEmployeeId();
            int nextNumber = 1;

            if (maxEmployeeId != null && maxEmployeeId.startsWith("TECH")) {
                try {
                    String numberPart = maxEmployeeId.substring(4); // Remove "TECH" prefix
                    nextNumber = Integer.parseInt(numberPart) + 1;
                } catch (NumberFormatException e) {
                    nextNumber = 1;
                }
            }

            return String.format("TECH%03d", nextNumber);
        } catch (Exception e) {
            // Fallback to timestamp-based ID
            return "TECH" + System.currentTimeMillis() % 1000;
        }
    }

    // API endpoints for AJAX operations

    @GetMapping("/api/roles")
    @ResponseBody
    public ResponseEntity<List<Role>> getAllRoles() {
        try {
            List<Role> roles = userService.getAllRoles();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            Optional<User> userOptional = userService.getUserById(id);
            if (userOptional.isPresent()) {
                return ResponseEntity.ok(userOptional.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // Additional user management methods from AdminUserController
    @GetMapping("/admin/users")
    @Transactional(readOnly = true)
    public String listUsers(@RequestParam(value = "search", required = false) String search,
            Model model) {
        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search);
        } else {
            users = userService.getAllUsers();
        }

        model.addAttribute("users", users);
        model.addAttribute("search", search);
        return "admin/users";
    }

    @GetMapping("/admin/users/new")
    @Transactional(readOnly = true)
    public String createUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", userService.getAllRoles());
        return "admin/user-form";
    }

    @GetMapping("/admin/users/edit/{id}")
    @Transactional(readOnly = true)
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getAllRoles());
        return "admin/user-form";
    }

    @PostMapping("/admin/users/save")
    @Transactional
    public String saveUser(@ModelAttribute User user,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "redirectTo", required = false) String redirectTo,
            RedirectAttributes redirectAttributes) {
        try {
            if (user.getId() == null) {
                // New user
                if (userService.existsByUsername(user.getUsername())) {
                    redirectAttributes.addFlashAttribute("error", "Username already exists!");
                    return "redirect:/admin/users/new";
                }
                if (userService.existsByEmail(user.getEmail())) {
                    redirectAttributes.addFlashAttribute("error", "Email already exists!");
                    return "redirect:/admin/users/new";
                }
                user.setCreatedAt(LocalDateTime.now());
                userService.saveUser(user);
                redirectAttributes.addFlashAttribute("message", "User created successfully!");
            } else {
                // Update existing user
                user.setUpdatedAt(LocalDateTime.now());
                userService.updateUser(user);
                redirectAttributes.addFlashAttribute("message", "User updated successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving user: " + e.getMessage());
        }

        // Redirect based on the redirectTo parameter
        if ("dashboard".equals(redirectTo)) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/admin/users";
    }

    @DeleteMapping("/admin/users/delete/{id}")
    @Transactional
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "User deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

}
