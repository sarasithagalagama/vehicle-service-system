package com.vehicleservice.service;

import com.vehicleservice.entity.Role;
import com.vehicleservice.entity.User;
import com.vehicleservice.entity.Technician;
import com.vehicleservice.repository.RoleRepository;
import com.vehicleservice.repository.UserRepository;
import com.vehicleservice.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@org.springframework.context.annotation.Scope("singleton")
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getAllUsersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsersPaginated(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findByUsernameOrEmailContaining(keyword, pageable);
    }

    public Page<User> getUsersWithFilters(String search, String role, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Convert status string to boolean
        Boolean isActive = null;
        if (status != null && !status.trim().isEmpty()) {
            isActive = "active".equals(status);
        }

        // Handle all combinations of filters
        if (search != null && !search.trim().isEmpty() && role != null && !role.trim().isEmpty() && isActive != null) {
            // Search + Role + Status
            return userRepository.findByUsernameOrEmailContainingAndRoleRoleNameAndIsActive(search, role, isActive,
                    pageable);
        } else if (search != null && !search.trim().isEmpty() && role != null && !role.trim().isEmpty()) {
            // Search + Role
            return userRepository.findByUsernameOrEmailContainingAndRoleRoleName(search, role, pageable);
        } else if (search != null && !search.trim().isEmpty() && isActive != null) {
            // Search + Status
            return userRepository.findByUsernameOrEmailContainingAndIsActive(search, isActive, pageable);
        } else if (role != null && !role.trim().isEmpty() && isActive != null) {
            // Role + Status
            return userRepository.findByRoleRoleNameAndIsActive(role, isActive, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            // Search only
            return userRepository.findByUsernameOrEmailContaining(search, pageable);
        } else if (role != null && !role.trim().isEmpty()) {
            // Role only
            return userRepository.findByRoleRoleName(role, pageable);
        } else if (isActive != null) {
            // Status only
            return userRepository.findByIsActive(isActive, pageable);
        } else {
            // No filters
            return userRepository.findAll(pageable);
        }
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // Keep existing password if not provided
            User existingUser = userRepository.findById(user.getId()).orElse(null);
            if (existingUser != null) {
                user.setPassword(existingUser.getPassword());
            }
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        // First, check if the user is a technician and delete the technician record
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // If user is a technician, delete the technician record first
            if (user.isTechnician()) {
                Optional<Technician> technicianOpt = technicianRepository.findByUser_Id(id);
                if (technicianOpt.isPresent()) {
                    technicianRepository.delete(technicianOpt.get());
                }
            }
        }

        // Now delete the user
        userRepository.deleteById(id);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.findByUsernameOrEmailContaining(keyword);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> getRoleByName(String roleName) {
        return roleRepository.findByRoleName(roleName);
    }

    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + id));
    }

    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    public List<User> getUsersByRole(String roleName) {
        try {
            return userRepository.findByRoleName(roleName);
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Return the user object - Spring Security will check isEnabled() method
        // The User entity's isEnabled() method already handles the isActive check
        return user;
    }

    public long getUserCount() {
        return userRepository.count();
    }

    public long getUserCountByRole(String roleName) {
        List<User> users = getUsersByRole(roleName);
        return users.size();
    }

}
