package com.vehicleservice.controller;

import com.vehicleservice.entity.User;
import com.vehicleservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@org.springframework.context.annotation.Scope("singleton")
public class StaffController {

    @Autowired
    private UserService userService;

    /// Staff dashboard main endpoint - routes to appropriate dashboard based on role
    @GetMapping("/staff/dashboard")
    public String staffDashboardDirect(Authentication authentication, Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            CsrfToken csrfToken) {
        try {
            String username = authentication != null ? authentication.getName() : null;

            if (username == null) {
                return "redirect:/login?error=Authentication failed";
            }

            User user = userService.getUserByUsername(username).orElse(null);

            if (user == null) {
                return "redirect:/login?error=User not found";
            }

            // Add user to model for all dashboards
            model.addAttribute("user", user);

            // Add CSRF token to model
            if (csrfToken != null) {
                model.addAttribute("_csrf", csrfToken);
            }

            // Add filter parameters to model for form persistence
            model.addAttribute("search", search);
            model.addAttribute("status", status);
            model.addAttribute("serviceType", serviceType);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);

            // Return appropriate dashboard based on user role
            String role = user.getRole() != null ? user.getRole().getRoleName().toUpperCase() : "RECEPTIONIST";

            switch (role) {
                case "MANAGER":
                    return "redirect:/manager/dashboard"; // Manager should go to manager dashboard
                case "RECEPTIONIST":
                    return "redirect:/staff/receptionist/dashboard";
                case "TECHNICIAN":
                    return "redirect:/staff/technician/dashboard";
                case "INVENTORY_MANAGER":
                    return "redirect:/staff/inventory-manager/dashboard";
                case "CUSTOMER":
                    return "redirect:/customer/dashboard";
                default:
                    return "redirect:/staff/receptionist/dashboard";
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while loading the dashboard", e);
        }
    }
}