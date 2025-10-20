package com.vehicleservice.service;

import com.vehicleservice.entity.Booking;
import com.vehicleservice.entity.Feedback;
import com.vehicleservice.entity.User;
import com.vehicleservice.repository.BookingRepository;
import com.vehicleservice.repository.FeedbackRepository;
import com.vehicleservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@org.springframework.context.annotation.Scope("singleton")
public class FeedbackService {
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAllActive();
    }

    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    public List<Feedback> getFeedbacksByBookingId(Long bookingId) {
        return feedbackRepository.findByBookingId(bookingId);
    }

    public List<Feedback> getFeedbacksByUserId(Long userId) {
        return feedbackRepository.findByUserId(userId);
    }

    public Feedback addFeedback(Feedback feedback) {
        // Validate that booking exists
        Booking booking = bookingRepository.findById(feedback.getBooking().getId())
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + feedback.getBooking().getId()));

        // Validate that user exists
        User user = userRepository.findById(feedback.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + feedback.getUser().getId()));

        // Check if active feedback already exists for this booking
        if (feedbackRepository.existsByBookingId(booking.getId())) {
            throw new RuntimeException("Feedback already exists for booking id: " + booking.getId());
        }

        // Validate rating range (assuming 1-5 scale)
        if (feedback.getRating() < 1 || feedback.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        feedback.setBooking(booking);
        feedback.setUser(user);
        feedback.updateTimestamp(); // Update timestamp before saving

        return feedbackRepository.save(feedback);
    }

    public Feedback updateFeedback(Long id, Feedback feedbackDetails) {
        Feedback existingFeedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + id));

        // Update rating if provided
        if (feedbackDetails.getRating() != null) {
            if (feedbackDetails.getRating() < 1 || feedbackDetails.getRating() > 5) {
                throw new RuntimeException("Rating must be between 1 and 5");
            }
            existingFeedback.setRating(feedbackDetails.getRating());
        }

        // Update comment if provided
        if (feedbackDetails.getComment() != null) {
            existingFeedback.setComment(feedbackDetails.getComment());
        }

        // Note: Booking and User relationships are typically not updated in feedback
        existingFeedback.updateTimestamp(); // Update timestamp before saving

        return feedbackRepository.save(existingFeedback);
    }

    public void deleteFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + id));

        // Hard delete to allow re-adding feedback for the same booking
        feedbackRepository.delete(feedback);
    }

    public boolean existsByBookingId(Long bookingId) {
        return feedbackRepository.existsByBookingId(bookingId);
    }

    /// Find inactive feedback for a booking (for potential reactivation)
    public Optional<Feedback> findInactiveFeedbackByBookingId(Long bookingId) {
        return feedbackRepository.findAllIncludingInactive().stream()
                .filter(f -> f.getBooking().getId().equals(bookingId) && !f.getIsActive())
                .findFirst();
    }
}
