package com.vehicleservice.controller;

import com.vehicleservice.entity.Role;
import com.vehicleservice.entity.User;
import com.vehicleservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Authentication Controller
 * Handles user login, registration, and role-based redirection
 */
@Controller
@org.springframework.context.annotation.Scope("singleton")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * Display login page
     * 
     * @param error  Error parameter from failed login attempt
     * @param logout Logout parameter from successful logout
     * @param model  Model to add attributes
     * @return Login page template
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password!");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    /**
     * Display registration page
     * 
     * @param model Model to add attributes
     * @return Registration page template
     */
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    /**
     * Process user registration
     * 
     * @param user               User object from form
     * @param redirectAttributes Flash attributes for redirect
     * @return Redirect to login page on success, registration page on error
     */
    @PostMapping("/register")
    public String registerUser(User user, RedirectAttributes redirectAttributes) {
        try {
            // Server-side validation
            String validationError = validateRegistrationInput(user);
            if (validationError != null) {
                redirectAttributes.addFlashAttribute("error", validationError);
                return "redirect:/register";
            }

            // Check if username or email already exists
            if (userService.existsByUsername(user.getUsername())) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/register";
            }

            if (userService.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already exists!");
                return "redirect:/register";
            }

            // Check phone number if provided
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
                if (userService.existsByPhoneNumber(user.getPhoneNumber())) {
                    redirectAttributes.addFlashAttribute("error", "Phone number already exists!");
                    return "redirect:/register";
                }
            }

            // Set default role as CUSTOMER
            Role customerRole = userService.getRoleByName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Customer role not found"));
            user.setRole(customerRole);

            // Set user as active by default
            user.setIsActive(true);

            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    /**
     * Validate registration input
     * 
     * @param user User object to validate
     * @return Validation error message or null if valid
     */
    private String validateRegistrationInput(User user) {
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

        // Validate password
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return "Password is required";
        }
        if (!user.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$")) {
            return "Password must be at least 8 characters with uppercase, lowercase, and number";
        }

        // Validate phone number (optional)
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            if (!user.getPhoneNumber().matches("^[\\+]?[0-9\\s\\-\\(\\)]{7,20}$")) {
                return "Please enter a valid phone number";
            }
        }

        return null; // All validations passed
    }

    /**
     * Home page - show landing page for unauthenticated users, redirect
     * authenticated users to dashboard
     * 
     * @param authentication Current user authentication
     * @return Home page template or redirect to appropriate dashboard
     */
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = (User) authentication.getPrincipal();
            String role = user.getRole().getRoleName();

            // Normalize role name (trim whitespace and convert to uppercase)
            String normalizedRole = role != null ? role.trim().toUpperCase() : "";

            switch (normalizedRole) {
                case "ADMIN":
                    return "redirect:/admin/dashboard";
                case "MANAGER":
                    return "redirect:/staff/dashboard";
                case "RECEPTIONIST":
                    return "redirect:/staff/dashboard";
                case "TECHNICIAN":
                    return "redirect:/staff/dashboard";
                case "FUEL_STAFF":
                    return "redirect:/staff/dashboard";
                case "INVENTORY_MANAGER":
                    return "redirect:/staff/dashboard";
                case "CUSTOMER":
                    return "redirect:/customer/dashboard";
                default:
                    return "redirect:/login";
            }
        }
        return "index";
    }

    /**
     * Public home page
     * 
     * @return Home page template
     */
    @GetMapping("/home")
    public String homePage() {
        return "index";
    }
}
