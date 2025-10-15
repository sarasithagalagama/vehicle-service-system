package com.vehicleservice.controller;

import com.vehicleservice.entity.Feedback;
import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.User;
import com.vehicleservice.service.FeedbackService;
import com.vehicleservice.service.BookingService;
import com.vehicleservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/feedbacks")
@org.springframework.context.annotation.Scope("singleton")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    // Display all feedbacks
    @GetMapping
    public String getAllFeedbacks(Model model) {
        List<Feedback> feedbacks = feedbackService.getAllFeedbacks();
        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("pageTitle", "All Feedbacks");
        return "manager/feedback-list";
    }

    // Display feedback details by ID
    @GetMapping("/{id}")
    public String getFeedbackById(@PathVariable Long id, Model model) {
        Optional<Feedback> feedback = feedbackService.getFeedbackById(id);
        if (feedback.isPresent()) {
            model.addAttribute("feedback", feedback.get());
            model.addAttribute("pageTitle", "Feedback Details");
            return "manager/feedback-details";
        } else {
            model.addAttribute("errorMessage", "Feedback not found with id: " + id);
            return "error/404";
        }
    }

    // Display feedbacks by booking ID
    @GetMapping("/booking/{bookingId}")
    public String getFeedbacksByBookingId(@PathVariable Long bookingId, Model model) {
        // Verify booking exists
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow();

        List<Feedback> feedbacks = feedbackService.getFeedbacksByBookingId(bookingId);
        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("booking", booking);
        model.addAttribute("pageTitle", "Feedbacks for Booking #" + bookingId);
        return "manager/feedback-list";
    }

    // Display feedbacks by user ID
    @GetMapping("/user/{userId}")
    public String getFeedbacksByUserId(@PathVariable Long userId, Model model) {
        // Verify user exists
        User user = userService.getUserById(userId)
                .orElseThrow();

        List<Feedback> feedbacks = feedbackService.getFeedbacksByUserId(userId);
        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "Feedbacks by User: " + user.getUsername());
        return "manager/feedback-list";

    }

    // Show form for creating new feedback
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("feedback", new Feedback());
        model.addAttribute("bookings", bookingService.getAllBookings());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pageTitle", "Add New Feedback");
        return "feedback/feedback-form";
    }

    // Show form for creating feedback for a specific booking
    @GetMapping("/new/booking/{bookingId}")
    public String showCreateFormForBooking(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow();

        Feedback feedback = new Feedback();
        feedback.setBooking(booking);

        model.addAttribute("feedback", feedback);
        model.addAttribute("bookings", List.of(booking));
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pageTitle", "Add Feedback for Booking #" + bookingId);
        return "feedback/feedback-form";

    }

    // Create new feedback
    @PostMapping
    public String createFeedback(@ModelAttribute Feedback feedback,
            RedirectAttributes redirectAttributes) {
        try {
            Feedback savedFeedback = feedbackService.addFeedback(feedback);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Feedback added successfully!");
            return "redirect:/feedbacks/" + savedFeedback.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error adding feedback: " + e.getMessage());
            return "redirect:/feedbacks/new";
        }
    }

    // Show form for editing feedback
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Feedback> feedback = feedbackService.getFeedbackById(id);
        if (feedback.isPresent()) {
            model.addAttribute("feedback", feedback.get());
            model.addAttribute("bookings", bookingService.getAllBookings());
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("pageTitle", "Edit Feedback");
            return "feedback/feedback-form";
        } else {
            model.addAttribute("errorMessage", "Feedback not found with id: " + id);
            return "error/404";
        }
    }

    // Update feedback
    @PostMapping("/update/{id}")
    public String updateFeedback(@PathVariable Long id,
            @ModelAttribute Feedback feedbackDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Feedback updatedFeedback = feedbackService.updateFeedback(id, feedbackDetails);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Feedback updated successfully!");
            return "redirect:/feedbacks/" + updatedFeedback.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error updating feedback: " + e.getMessage());
            return "redirect:/feedbacks/edit/" + id;
        }
    }

    // Show delete confirmation page
    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable Long id, Model model) {
        Optional<Feedback> feedback = feedbackService.getFeedbackById(id);
        if (feedback.isPresent()) {
            model.addAttribute("feedback", feedback.get());
            model.addAttribute("pageTitle", "Delete Feedback");
            return "feedback/feedback-delete";
        } else {
            model.addAttribute("errorMessage", "Feedback not found with id: " + id);
            return "error/404";
        }
    }

    // Delete feedback
    @DeleteMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        feedbackService.deleteFeedback(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Feedback deleted successfully!");
        return "redirect:/feedbacks";

    }

}
