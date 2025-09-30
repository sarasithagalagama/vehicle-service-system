-- Vehicle Service System Data Population Script
-- Run this script AFTER running create_database.sql
-- This script populates the database with sample data

USE VehicleServiceDB;
GO

-- Clear existing data (optional - uncomment if you want to reset data)
/*
DELETE FROM feedback;
DELETE FROM inventory_transactions;
DELETE FROM technician_assignments;
DELETE FROM bookings;
DELETE FROM inventory_items;
DELETE FROM technicians;
DELETE FROM users;
DELETE FROM roles;
*/

-- Insert Roles
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ADMIN')
BEGIN
    INSERT INTO roles (role_name, created_at, updated_at, is_active) VALUES
    ('ADMIN', GETDATE(), GETDATE(), 1),
    ('MANAGER', GETDATE(), GETDATE(), 1),
    ('RECEPTIONIST', GETDATE(), GETDATE(), 1),
    ('TECHNICIAN', GETDATE(), GETDATE(), 1),
    ('INVENTORY_MANAGER', GETDATE(), GETDATE(), 1),
    ('CUSTOMER', GETDATE(), GETDATE(), 1);
    PRINT 'Roles inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Roles already exist.';
END
GO

-- Insert Users
-- Note: Passwords are encoded with BCrypt. In production, use proper password encoding.
-- For demo purposes, using simple passwords that match the DataInitializationService
IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')
BEGIN
    -- Get role IDs
    DECLARE @adminRoleId BIGINT = (SELECT id FROM roles WHERE role_name = 'ADMIN');
    DECLARE @managerRoleId BIGINT = (SELECT id FROM roles WHERE role_name = 'MANAGER');
    DECLARE @receptionistRoleId BIGINT = (SELECT id FROM roles WHERE role_name = 'RECEPTIONIST');
    DECLARE @technicianRoleId BIGINT = (SELECT id FROM roles WHERE role_name = 'TECHNICIAN');
    DECLARE @inventoryManagerRoleId BIGINT = (SELECT id FROM roles WHERE role_name = 'INVENTORY_MANAGER');
    DECLARE @customerRoleId BIGINT = (SELECT id FROM roles WHERE role_name = 'CUSTOMER');

    INSERT INTO users (username, password, email, first_name, last_name, phone_number, address, city, zip_code, role_id, created_at, updated_at, is_active) VALUES
    -- Admin
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'admin@vehicleservice.com', 'John', 'Admin', '+94 11 234 5678', '123 Galle Road, Fort', 'Colombo 01', '00100', @adminRoleId, GETDATE(), GETDATE(), 1),
    
    -- Managers
    ('manager1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'manager1@vehicleservice.com', 'Robert', 'Manager', '+94 11 234 5683', '789 Galle Road, Fort', 'Colombo 01', '00100', @managerRoleId, GETDATE(), GETDATE(), 1),
    ('manager2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'manager2@vehicleservice.com', 'Jennifer', 'Thompson', '+94 11 234 5684', '456 Union Place, Maradana', 'Colombo 07', '00700', @managerRoleId, GETDATE(), GETDATE(), 1),
    
    -- Staff
    ('receptionist1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'receptionist1@vehicleservice.com', 'Sarah', 'Johnson', '+94 11 234 5679', '456 Union Place, Maradana', 'Colombo 07', '00700', @receptionistRoleId, GETDATE(), GETDATE(), 1),
    ('technician1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'technician1@vehicleservice.com', 'Mike', 'Wilson', '+94 11 234 5680', '789 Baseline Road, Borella', 'Colombo 08', '00800', @technicianRoleId, GETDATE(), GETDATE(), 1),
    ('technician2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'technician2@vehicleservice.com', 'Alex', 'Rodriguez', '+94 11 234 5685', '321 High Level Road, Nugegoda', 'Nugegoda', '10250', @technicianRoleId, GETDATE(), GETDATE(), 1),
    ('inventory1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'inventory1@vehicleservice.com', 'David', 'Lee', '+94 11 234 5682', '654 Negombo Road, Wattala', 'Wattala', '11104', @inventoryManagerRoleId, GETDATE(), GETDATE(), 1),
    
    -- Customers
    ('customer1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'customer1@vehicleservice.com', 'Alice', 'Smith', '+94 77 123 4567', '321 Peradeniya Road, Kandy', 'Kandy', '20000', @customerRoleId, GETDATE(), GETDATE(), 1),
    ('customer2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'customer2@vehicleservice.com', 'Bob', 'Brown', '+94 77 234 5678', '654 Galle Road, Fort', 'Galle', '80000', @customerRoleId, GETDATE(), GETDATE(), 1),
    ('customer3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'customer3@vehicleservice.com', 'Charlie', 'Wilson', '+94 77 345 6789', '789 Negombo Road, Wattala', 'Wattala', '11104', @customerRoleId, GETDATE(), GETDATE(), 1),
    ('customer4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'customer4@vehicleservice.com', 'Diana', 'Lee', '+94 77 456 7890', '456 High Level Road, Nugegoda', 'Nugegoda', '10250', @customerRoleId, GETDATE(), GETDATE(), 1),
    ('customer5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'customer5@vehicleservice.com', 'Eva', 'Martinez', '+94 77 567 8901', '123 Baseline Road, Borella', 'Colombo 08', '00800', @customerRoleId, GETDATE(), GETDATE(), 1);

    PRINT 'Users inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Users already exist.';
END
GO

-- Insert Technicians
IF NOT EXISTS (SELECT 1 FROM technicians WHERE employee_id = 'TECH001')
BEGIN
    DECLARE @technician1UserId BIGINT = (SELECT id FROM users WHERE username = 'technician1');
    DECLARE @technician2UserId BIGINT = (SELECT id FROM users WHERE username = 'technician2');

    INSERT INTO technicians (user_id, employee_id, specialization, max_daily_workload, current_workload, hourly_rate, experience_years, created_at, updated_at, is_active) VALUES
    (@technician1UserId, 'TECH001', 'Engine Repair & Maintenance', 6, 0, 1500.00, 3, GETDATE(), GETDATE(), 1),
    (@technician2UserId, 'TECH002', 'Brake & Transmission Systems', 5, 0, 1800.00, 5, GETDATE(), GETDATE(), 1);

    PRINT 'Technicians inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Technicians already exist.';
END
GO

-- Insert Inventory Items
IF NOT EXISTS (SELECT 1 FROM inventory_items WHERE item_name = 'Oil Filter')
BEGIN
    INSERT INTO inventory_items (item_name, category, quantity, unit_price, reorder_level, created_at, updated_at, is_active) VALUES
    ('Oil Filter', 'Engine Parts', 15, 25.00, 10, GETDATE(), GETDATE(), 1),
    ('Air Filter', 'Engine Parts', 8, 35.00, 12, GETDATE(), GETDATE(), 1),
    ('Brake Pads', 'Brake System', 5, 80.00, 8, GETDATE(), GETDATE(), 1),
    ('Engine Oil 5W-30', 'Fluids', 20, 45.00, 15, GETDATE(), GETDATE(), 1),
    ('Antifreeze Coolant', 'Fluids', 12, 30.00, 10, GETDATE(), GETDATE(), 1),
    ('Spark Plugs', 'Engine Parts', 25, 15.00, 20, GETDATE(), GETDATE(), 1),
    ('Brake Fluid DOT 4', 'Brake System', 8, 18.00, 5, GETDATE(), GETDATE(), 1),
    ('Windshield Wiper Blades', 'Body Parts', 12, 22.00, 8, GETDATE(), GETDATE(), 1),
    ('Car Battery 12V', 'Electrical', 3, 120.00, 2, GETDATE(), GETDATE(), 1),
    ('Alternator', 'Electrical', 2, 180.00, 1, GETDATE(), GETDATE(), 1);

    PRINT 'Inventory items inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Inventory items already exist.';
END
GO

-- Insert Bookings
IF NOT EXISTS (SELECT 1 FROM bookings WHERE booking_number = 'BK001')
BEGIN
    INSERT INTO bookings (booking_number, customer_name, vehicle_number, service_type, booking_date, payment_status, service_price, additional_charges, total_price, paid_amount, remaining_amount, notes, created_at, updated_at) VALUES
    ('BK001', 'Alice Smith', 'ABC-1234', 'oil_change', DATEADD(day, 1, GETDATE()), 'PENDING', 2500.00, 0.00, 2500.00, 0.00, 2500.00, 'Regular maintenance - 5000km service', GETDATE(), GETDATE()),
    ('BK002', 'Bob Brown', 'XYZ-7890', 'brake_service', DATEADD(day, 2, GETDATE()), 'PARTIAL', 8000.00, 500.00, 8500.00, 3000.00, 5500.00, 'Squeaking noise when braking - needs inspection', GETDATE(), GETDATE()),
    ('BK003', 'Alice Smith', 'DEF-4567', 'engine_tune', DATEADD(day, -1, GETDATE()), 'PAID', 12000.00, 1500.00, 13500.00, 13500.00, 0.00, 'Rough idle and poor fuel economy - diagnostic needed', GETDATE(), GETDATE()),
    ('BK004', 'Charlie Wilson', 'GHI-2345', 'transmission', DATEADD(day, 3, GETDATE()), 'PENDING', 15000.00, 0.00, 15000.00, 0.00, 15000.00, 'Transmission fluid change and filter replacement', GETDATE(), GETDATE()),
    ('BK005', 'Diana Lee', 'JKL-6789', 'ac_service', DATEADD(day, 1, GETDATE()), 'PARTIAL', 6000.00, 800.00, 6800.00, 2000.00, 4800.00, 'AC not cooling properly - needs refrigerant recharge', GETDATE(), GETDATE());

    PRINT 'Bookings inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Bookings already exist.';
END
GO

-- Insert Inventory Transactions
IF NOT EXISTS (SELECT 1 FROM inventory_transactions WHERE id = 1)
BEGIN
    DECLARE @inventoryManagerId BIGINT = (SELECT id FROM users WHERE username = 'inventory1');
    DECLARE @technician1Id BIGINT = (SELECT id FROM users WHERE username = 'technician1');
    DECLARE @oilFilterId BIGINT = (SELECT id FROM inventory_items WHERE item_name = 'Oil Filter');
    DECLARE @airFilterId BIGINT = (SELECT id FROM inventory_items WHERE item_name = 'Air Filter');
    DECLARE @brakePadId BIGINT = (SELECT id FROM inventory_items WHERE item_name = 'Brake Pads');

    INSERT INTO inventory_transactions (item_id, transaction_type, quantity, date, staff_id, created_at, updated_at, is_active) VALUES
    (@oilFilterId, 'IN', 20, DATEADD(day, -5, GETDATE()), @inventoryManagerId, GETDATE(), GETDATE(), 1),
    (@airFilterId, 'OUT', 3, DATEADD(day, -2, GETDATE()), @technician1Id, GETDATE(), GETDATE(), 1),
    (@brakePadId, 'OUT', 2, DATEADD(day, -1, GETDATE()), @inventoryManagerId, GETDATE(), GETDATE(), 1);

    PRINT 'Inventory transactions inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Inventory transactions already exist.';
END
GO

-- Insert Sample Feedback
IF NOT EXISTS (SELECT 1 FROM feedback WHERE id = 1)
BEGIN
    DECLARE @booking1Id BIGINT = (SELECT id FROM bookings WHERE booking_number = 'BK001');
    DECLARE @booking2Id BIGINT = (SELECT id FROM bookings WHERE booking_number = 'BK002');
    DECLARE @customer1Id BIGINT = (SELECT id FROM users WHERE username = 'customer1');
    DECLARE @customer2Id BIGINT = (SELECT id FROM users WHERE username = 'customer2');

    INSERT INTO feedback (rating, comment, bookings_id, users_id, created_at, updated_at, is_active) VALUES
    (5, 'Excellent service! Very professional and quick.', @booking1Id, @customer1Id, GETDATE(), GETDATE(), 1),
    (4, 'Good service, but took longer than expected.', @booking2Id, @customer2Id, GETDATE(), GETDATE(), 1);

    PRINT 'Feedback inserted successfully.';
END
ELSE
BEGIN
    PRINT 'Feedback already exist.';
END
GO

-- Display Summary
PRINT '';
PRINT '=== DATA POPULATION SUMMARY ===';
PRINT 'Roles: ' + CAST((SELECT COUNT(*) FROM roles) AS VARCHAR(10));
PRINT 'Users: ' + CAST((SELECT COUNT(*) FROM users) AS VARCHAR(10));
PRINT 'Technicians: ' + CAST((SELECT COUNT(*) FROM technicians) AS VARCHAR(10));
PRINT 'Bookings: ' + CAST((SELECT COUNT(*) FROM bookings) AS VARCHAR(10));
PRINT 'Inventory Items: ' + CAST((SELECT COUNT(*) FROM inventory_items) AS VARCHAR(10));
PRINT 'Inventory Transactions: ' + CAST((SELECT COUNT(*) FROM inventory_transactions) AS VARCHAR(10));
PRINT 'Feedback: ' + CAST((SELECT COUNT(*) FROM feedback) AS VARCHAR(10));
PRINT '';
PRINT '=== LOGIN CREDENTIALS ===';
PRINT 'Admin: admin / admin123';
PRINT 'Manager: manager1 / manager123';
PRINT 'Receptionist: receptionist1 / staff123';
PRINT 'Technician: technician1 / staff123';
PRINT 'Inventory Manager: inventory1 / staff123';
PRINT 'Customer: customer1 / customer123';
PRINT '';
PRINT 'Database setup completed successfully!';
PRINT 'You can now run the Spring Boot application.';
GO
