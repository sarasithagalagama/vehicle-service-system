package com.vehicleservice.service;

import com.vehicleservice.entity.*;
import com.vehicleservice.repository.RoleRepository;
import com.vehicleservice.repository.UserRepository;
import com.vehicleservice.repository.BookingRepository;
import com.vehicleservice.repository.InventoryItemRepository;
import com.vehicleservice.repository.InventoryTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;


    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AssignmentService technicianAssignmentService;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeUsers();
        initializeInventoryItems();
        initializeBookings();
        initializeInventoryTransactions();
    }

    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            Role adminRole = new Role("ADMIN");
            Role managerRole = new Role("MANAGER");
            Role receptionistRole = new Role("RECEPTIONIST");
            Role technicianRole = new Role("TECHNICIAN");
            Role inventoryManagerRole = new Role("INVENTORY_MANAGER");
            Role customerRole = new Role("CUSTOMER");

            roleRepository.saveAll(Arrays.asList(adminRole, managerRole, receptionistRole, technicianRole, inventoryManagerRole, customerRole));
        }
    }

    private void initializeUsers() {
        // Always check and create manager users if they don't exist
        initializeManagerUsers();
        
        // Check if we need to create other users (not just managers)
        if (userRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = roleRepository.findByRoleName("ADMIN").orElse(null);
            Role managerRole = roleRepository.findByRoleName("MANAGER").orElse(null);
            Role receptionistRole = roleRepository.findByRoleName("RECEPTIONIST").orElse(null);
            Role technicianRole = roleRepository.findByRoleName("TECHNICIAN").orElse(null);
            Role inventoryManagerRole = roleRepository.findByRoleName("INVENTORY_MANAGER").orElse(null);
            Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElse(null);

            // Create admin user
            User admin = new User("admin", passwordEncoder.encode("admin123"), "admin@vehicleservice.com", "John", "Admin", adminRole);
            admin.setPhoneNumber("+94 11 234 5678");
            admin.setAddress("123 Galle Road, Fort");
            admin.setCity("Colombo 01");
            admin.setZipCode("00100");

            // Create manager users
            User manager1 = new User("manager1", passwordEncoder.encode("manager123"), "manager1@vehicleservice.com", "Robert", "Manager", managerRole);
            manager1.setPhoneNumber("+94 11 234 5683");
            manager1.setAddress("789 Galle Road, Fort");
            manager1.setCity("Colombo 01");
            manager1.setZipCode("00100");

            User manager2 = new User("manager2", passwordEncoder.encode("manager123"), "manager2@vehicleservice.com", "Jennifer", "Thompson", managerRole);
            manager2.setPhoneNumber("+94 11 234 5684");
            manager2.setAddress("456 Union Place, Maradana");
            manager2.setCity("Colombo 07");
            manager2.setZipCode("00700");

            // Create staff users with specific roles
            User receptionist1 = new User("receptionist1", passwordEncoder.encode("staff123"), "receptionist1@vehicleservice.com", "Sarah", "Johnson", receptionistRole);
            receptionist1.setPhoneNumber("+94 11 234 5679");
            receptionist1.setAddress("456 Union Place, Maradana");
            receptionist1.setCity("Colombo 07");
            receptionist1.setZipCode("00700");

            User technician1 = new User("technician1", passwordEncoder.encode("staff123"), "technician1@vehicleservice.com", "Mike", "Wilson", technicianRole);
            technician1.setPhoneNumber("+94 11 234 5680");
            technician1.setAddress("789 Baseline Road, Borella");
            technician1.setCity("Colombo 08");
            technician1.setZipCode("00800");

            User technician2 = new User("technician2", passwordEncoder.encode("staff123"), "technician2@vehicleservice.com", "Alex", "Rodriguez", technicianRole);
            technician2.setPhoneNumber("+94 11 234 5685");
            technician2.setAddress("321 High Level Road, Nugegoda");
            technician2.setCity("Nugegoda");
            technician2.setZipCode("10250");


            User inventoryManager1 = new User("inventory1", passwordEncoder.encode("staff123"), "inventory1@vehicleservice.com", "David", "Lee", inventoryManagerRole);
            inventoryManager1.setPhoneNumber("+94 11 234 5682");
            inventoryManager1.setAddress("654 Negombo Road, Wattala");
            inventoryManager1.setCity("Wattala");
            inventoryManager1.setZipCode("11104");

            // Create customer users
            User customer1 = new User("customer1", passwordEncoder.encode("customer123"), "customer1@vehicleservice.com", "Alice", "Smith", customerRole);
            customer1.setPhoneNumber("+94 77 123 4567");
            customer1.setAddress("321 Peradeniya Road, Kandy");
            customer1.setCity("Kandy");
            customer1.setZipCode("20000");

            User customer2 = new User("customer2", passwordEncoder.encode("customer123"), "customer2@vehicleservice.com", "Bob", "Brown", customerRole);
            customer2.setPhoneNumber("+94 77 234 5678");
            customer2.setAddress("654 Galle Road, Fort");
            customer2.setCity("Galle");
            customer2.setZipCode("80000");

            User customer3 = new User("customer3", passwordEncoder.encode("customer123"), "customer3@vehicleservice.com", "Charlie", "Wilson", customerRole);
            customer3.setPhoneNumber("+94 77 345 6789");
            customer3.setAddress("789 Negombo Road, Wattala");
            customer3.setCity("Wattala");
            customer3.setZipCode("11104");

            User customer4 = new User("customer4", passwordEncoder.encode("customer123"), "customer4@vehicleservice.com", "Diana", "Lee", customerRole);
            customer4.setPhoneNumber("+94 77 456 7890");
            customer4.setAddress("456 High Level Road, Nugegoda");
            customer4.setCity("Nugegoda");
            customer4.setZipCode("10250");

            User customer5 = new User("customer5", passwordEncoder.encode("customer123"), "customer5@vehicleservice.com", "Eva", "Martinez", customerRole);
            customer5.setPhoneNumber("+94 77 567 8901");
            customer5.setAddress("123 Baseline Road, Borella");
            customer5.setCity("Colombo 08");
            customer5.setZipCode("00800");

            userRepository.saveAll(Arrays.asList(admin, manager1, manager2, receptionist1, technician1, technician2, inventoryManager1, 
                customer1, customer2, customer3, customer4, customer5));
            
            // Create technician entities for technician users
            technicianAssignmentService.createTechnician(technician1, "TECH001", "Engine Repair & Maintenance", 6, new BigDecimal("1500.00"), 3);
            technicianAssignmentService.createTechnician(technician2, "TECH002", "Brake & Transmission Systems", 5, new BigDecimal("1800.00"), 5);
        }
    }

    private void initializeManagerUsers() {
        // Check if manager users exist, if not create them
        if (!userRepository.findByUsername("manager1").isPresent()) {
            Role managerRole = roleRepository.findByRoleName("MANAGER").orElse(null);
            if (managerRole != null) {
                User manager1 = new User("manager1", passwordEncoder.encode("manager123"), "manager1@vehicleservice.com", "Robert", "Manager", managerRole);
                manager1.setPhoneNumber("+94 11 234 5683");
                manager1.setAddress("789 Galle Road, Fort");
                manager1.setCity("Colombo 01");
                manager1.setZipCode("00100");
                userRepository.save(manager1);
            }
        }

        if (!userRepository.findByUsername("manager2").isPresent()) {
            Role managerRole = roleRepository.findByRoleName("MANAGER").orElse(null);
            if (managerRole != null) {
                User manager2 = new User("manager2", passwordEncoder.encode("manager123"), "manager2@vehicleservice.com", "Jennifer", "Thompson", managerRole);
                manager2.setPhoneNumber("+94 11 234 5684");
                manager2.setAddress("456 Union Place, Maradana");
                manager2.setCity("Colombo 07");
                manager2.setZipCode("00700");
                userRepository.save(manager2);
            }
        }
    }

    private void initializeInventoryItems() {
        if (inventoryItemRepository.count() == 0) {
            InventoryItem oilFilter = new InventoryItem("Oil Filter", "Engine Parts", 15, new BigDecimal("25.00"), 10);
            InventoryItem airFilter = new InventoryItem("Air Filter", "Engine Parts", 8, new BigDecimal("35.00"), 12);
            InventoryItem brakePad = new InventoryItem("Brake Pads", "Brake System", 5, new BigDecimal("80.00"), 8);
            InventoryItem engineOil = new InventoryItem("Engine Oil 5W-30", "Fluids", 20, new BigDecimal("45.00"), 15);
            InventoryItem coolant = new InventoryItem("Antifreeze Coolant", "Fluids", 12, new BigDecimal("30.00"), 10);
            InventoryItem sparkPlug = new InventoryItem("Spark Plugs", "Engine Parts", 25, new BigDecimal("15.00"), 20);
            InventoryItem brakeFluid = new InventoryItem("Brake Fluid DOT 4", "Brake System", 8, new BigDecimal("18.00"), 5);
            InventoryItem wiperBlades = new InventoryItem("Windshield Wiper Blades", "Body Parts", 12, new BigDecimal("22.00"), 8);
            InventoryItem battery = new InventoryItem("Car Battery 12V", "Electrical", 3, new BigDecimal("120.00"), 2);
            InventoryItem alternator = new InventoryItem("Alternator", "Electrical", 2, new BigDecimal("180.00"), 1);

            inventoryItemRepository.saveAll(Arrays.asList(oilFilter, airFilter, brakePad, engineOil, coolant, sparkPlug, brakeFluid, wiperBlades, battery, alternator));
        }
    }

    private void initializeBookings() {
        // Only add sample bookings if none exist
        if (bookingRepository.count() == 0) {
            // Get technicians for assignment (if needed in future)
            // User technician1 = userRepository.findByUsername("technician1").orElse(null);
            // User technician2 = userRepository.findByUsername("technician2").orElse(null);

            // Create only 5 sample bookings for demonstration
            Booking booking1 = new Booking("BK001", "Alice Smith", "ABC-1234", "oil_change", 
                LocalDateTime.now().plusDays(1).withHour(9).withMinute(0), "Regular maintenance - 5000km service");
            booking1.setPaymentStatus(Booking.PaymentStatus.PENDING);
            booking1.setServicePrice(new BigDecimal("2500.00"));
            booking1.setAdditionalCharges(new BigDecimal("0.00"));
            booking1.setTotalPrice(new BigDecimal("2500.00"));

            Booking booking2 = new Booking("BK002", "Bob Brown", "XYZ-7890", "brake_service", 
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(30), "Squeaking noise when braking - needs inspection");
            booking2.setPaymentStatus(Booking.PaymentStatus.PARTIAL);
            booking2.setServicePrice(new BigDecimal("8000.00"));
            booking2.setAdditionalCharges(new BigDecimal("500.00"));
            booking2.setTotalPrice(new BigDecimal("8500.00"));

            Booking booking3 = new Booking("BK003", "Alice Smith", "DEF-4567", "engine_tune", 
                LocalDateTime.now().minusDays(1).withHour(14).withMinute(0), "Rough idle and poor fuel economy - diagnostic needed");
            booking3.setPaymentStatus(Booking.PaymentStatus.PAID);
            booking3.setServicePrice(new BigDecimal("12000.00"));
            booking3.setAdditionalCharges(new BigDecimal("1500.00"));
            booking3.setTotalPrice(new BigDecimal("13500.00"));

            Booking booking4 = new Booking("BK004", "Charlie Wilson", "GHI-2345", "transmission", 
                LocalDateTime.now().plusDays(3).withHour(11).withMinute(0), "Transmission fluid change and filter replacement");
            booking4.setPaymentStatus(Booking.PaymentStatus.PENDING);
            booking4.setServicePrice(new BigDecimal("15000.00"));
            booking4.setAdditionalCharges(new BigDecimal("0.00"));
            booking4.setTotalPrice(new BigDecimal("15000.00"));

            Booking booking5 = new Booking("BK005", "Diana Lee", "JKL-6789", "ac_service", 
                LocalDateTime.now().plusDays(1).withHour(15).withMinute(30), "AC not cooling properly - needs refrigerant recharge");
            booking5.setPaymentStatus(Booking.PaymentStatus.PARTIAL);
            booking5.setServicePrice(new BigDecimal("6000.00"));
            booking5.setAdditionalCharges(new BigDecimal("800.00"));
            booking5.setTotalPrice(new BigDecimal("6800.00"));

            bookingRepository.saveAll(Arrays.asList(booking1, booking2, booking3, booking4, booking5));
        }
    }

    

    private void initializeInventoryTransactions() {
        if (inventoryTransactionRepository.count() == 0) {
            User inventoryManager1 = userRepository.findByUsername("inventory1").orElse(null);
            User technician1 = userRepository.findByUsername("technician1").orElse(null);

            // Only create transactions if both users exist
            if (inventoryManager1 != null && technician1 != null) {
                InventoryItem oilFilter = inventoryItemRepository.findByItemName("Oil Filter").orElse(null);
                InventoryItem airFilter = inventoryItemRepository.findByItemName("Air Filter").orElse(null);
                InventoryItem brakePad = inventoryItemRepository.findByItemName("Brake Pads").orElse(null);

                if (oilFilter != null && airFilter != null && brakePad != null) {
                    InventoryTransaction transaction1 = new InventoryTransaction(oilFilter, 
                        InventoryTransaction.TransactionType.IN, 20, LocalDateTime.now().minusDays(5), inventoryManager1);

                    InventoryTransaction transaction2 = new InventoryTransaction(airFilter, 
                        InventoryTransaction.TransactionType.OUT, 3, LocalDateTime.now().minusDays(2), technician1);

                    InventoryTransaction transaction3 = new InventoryTransaction(brakePad, 
                        InventoryTransaction.TransactionType.OUT, 2, LocalDateTime.now().minusDays(1), inventoryManager1);

                    inventoryTransactionRepository.saveAll(Arrays.asList(transaction1, transaction2, transaction3));
                }
            }
        }
    }

    
}
