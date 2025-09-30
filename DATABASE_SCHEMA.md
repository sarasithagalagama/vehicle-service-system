# Vehicle Service System - Database Schema

## Overview
This document describes the complete database schema for the Vehicle Service System. The system uses JPA/Hibernate with MySQL/SQL Server as the underlying database.

## Base Entity
All entities extend `BaseEntity` which provides common audit fields:
- `created_at` (LocalDateTime) - Record creation timestamp
- `updated_at` (LocalDateTime) - Last modification timestamp  
- `is_active` (Boolean) - Soft delete flag (default: true)

---

## Tables

### 1. roles
**Purpose**: Defines user roles and permissions in the system

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique role identifier |
| role_name | VARCHAR(255) | UNIQUE, NOT NULL | Role name (ADMIN, MANAGER, etc.) |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete flag |

**Predefined Roles:**
- `ADMIN` - System administrators
- `MANAGER` - Service managers  
- `RECEPTIONIST` - Front desk staff
- `TECHNICIAN` - Vehicle technicians
- `INVENTORY_MANAGER` - Inventory management staff
- `CUSTOMER` - Service customers

---

### 2. users
**Purpose**: Stores all system users (staff and customers)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique user identifier |
| username | VARCHAR(255) | UNIQUE, NOT NULL | Login username |
| password | VARCHAR(255) | NOT NULL | Encrypted password |
| email | VARCHAR(255) | UNIQUE, NOT NULL | User email address |
| first_name | VARCHAR(255) | NOT NULL | User's first name |
| last_name | VARCHAR(255) | NOT NULL | User's last name |
| phone_number | VARCHAR(20) | UNIQUE | Contact phone number |
| address | VARCHAR(500) | | Street address |
| city | VARCHAR(100) | | City name |
| state | VARCHAR(100) | | State/Province |
| zip_code | VARCHAR(20) | | Postal/ZIP code |
| date_of_birth | DATETIME | | User's date of birth |
| last_login | DATETIME | | Last login timestamp |
| role_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to roles table |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete flag |

**Relationships:**
- Many-to-One with `roles` (role_id → roles.id)

---

### 3. technicians
**Purpose**: Extended information for technician users

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique technician identifier |
| user_id | BIGINT | FOREIGN KEY, UNIQUE, NOT NULL | Reference to users table |
| employee_id | VARCHAR(50) | UNIQUE, NOT NULL | Employee ID (e.g., TECH001) |
| specialization | VARCHAR(255) | | Technician's area of expertise |
| max_daily_workload | INT | NOT NULL, DEFAULT 6 | Maximum daily assignments |
| current_workload | INT | NOT NULL, DEFAULT 0 | Current active assignments |
| hourly_rate | DECIMAL(10,2) | | Hourly wage rate |
| experience_years | INT | DEFAULT 0 | Years of experience |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete flag |

**Relationships:**
- One-to-One with `users` (user_id → users.id)

---

### 4. bookings
**Purpose**: Service appointment bookings

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique booking identifier |
| booking_number | VARCHAR(50) | UNIQUE, NOT NULL | Booking reference number |
| customer_name | VARCHAR(100) | NOT NULL | Customer's full name |
| vehicle_number | VARCHAR(20) | NOT NULL | Vehicle registration number |
| service_type | VARCHAR(100) | NOT NULL | Type of service requested |
| booking_date | DATETIME | NOT NULL | Scheduled appointment date/time |
| payment_status | ENUM | NOT NULL | Payment status (PENDING, PAID, PARTIAL, REFUNDED) |
| service_price | DECIMAL(10,2) | | Base service price |
| additional_charges | DECIMAL(10,2) | | Extra charges |
| total_price | DECIMAL(10,2) | NOT NULL | Total amount |
| paid_amount | DECIMAL(10,2) | | Amount paid so far |
| remaining_amount | DECIMAL(10,2) | | Outstanding balance |
| notes | VARCHAR(1000) | | Additional notes/comments |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |

**Service Types:**
- `oil_change` - Regular oil change service
- `brake_service` - Brake system maintenance
- `engine_tune` - Engine tuning and diagnostics
- `transmission` - Transmission service
- `ac_service` - Air conditioning service

---

### 5. technician_assignments
**Purpose**: Links technicians to specific bookings

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique assignment identifier |
| booking_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to bookings table |
| technician_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to technicians table |
| assigned_by | BIGINT | FOREIGN KEY, NOT NULL | User who made the assignment |
| assignment_date | DATETIME | NOT NULL | When assignment was made |
| status | ENUM | NOT NULL | Assignment status (ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED) |
| notes | VARCHAR(500) | | Assignment notes |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete flag |

**Relationships:**
- Many-to-One with `bookings` (booking_id → bookings.id)
- Many-to-One with `technicians` (technician_id → technicians.id)
- Many-to-One with `users` (assigned_by → users.id)

---

### 6. inventory_items
**Purpose**: Parts and supplies inventory

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique item identifier |
| item_name | VARCHAR(255) | NOT NULL | Item name/description |
| category | VARCHAR(100) | NOT NULL | Item category |
| quantity | INT | NOT NULL | Current stock quantity |
| unit_price | DECIMAL(10,2) | NOT NULL | Price per unit |
| reorder_level | INT | NOT NULL | Minimum stock level for reorder |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete flag |

**Categories:**
- `Engine Parts` - Oil filters, air filters, spark plugs
- `Brake System` - Brake pads, brake fluid
- `Fluids` - Engine oil, coolant, transmission fluid
- `Electrical` - Batteries, alternators, wiring
- `Body Parts` - Wiper blades, mirrors, lights

---

### 7. inventory_transactions
**Purpose**: Tracks all inventory movements

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique transaction identifier |
| item_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to inventory_items table |
| transaction_type | ENUM | NOT NULL | Transaction type (IN, OUT) |
| quantity | INT | NOT NULL | Quantity moved |
| date | DATETIME | NOT NULL | Transaction date/time |
| staff_id | BIGINT | FOREIGN KEY, NOT NULL | Staff member who performed transaction |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete flag |

**Transaction Types:**
- `IN` - Stock addition (purchases, returns)
- `OUT` - Stock usage (service consumption, sales)

**Relationships:**
- Many-to-One with `inventory_items` (item_id → inventory_items.id)
- Many-to-One with `users` (staff_id → users.id)

---

### 8. feedback
**Purpose**: Customer feedback and ratings

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique feedback identifier |
| rating | INT | NOT NULL | Rating (1-5 stars) |
| comment | VARCHAR(500) | | Customer comment |
| bookings_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to bookings table |
| users_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to users table |
| created_at | DATETIME | NOT NULL | Record creation timestamp |
| updated_at | DATETIME | NOT NULL | Last modification timestamp |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Soft delete flag |

**Relationships:**
- Many-to-One with `bookings` (bookings_id → bookings.id)
- Many-to-One with `users` (users_id → users.id)

---

## Entity Relationships Diagram

```
roles (1) ←→ (M) users
users (1) ←→ (1) technicians
bookings (1) ←→ (M) technician_assignments (M) ←→ (1) technicians
bookings (1) ←→ (M) feedback (M) ←→ (1) users
inventory_items (1) ←→ (M) inventory_transactions (M) ←→ (1) users
```

## Key Features

### 1. **Audit Trail**
- All entities track creation and modification timestamps
- Soft delete functionality with `is_active` flag
- Automatic timestamp updates on modifications

### 2. **User Management**
- Role-based access control (RBAC)
- Comprehensive user profiles with contact information
- Specialized technician profiles with workload management

### 3. **Booking System**
- Flexible service types
- Payment tracking with multiple statuses
- Automatic price calculations

### 4. **Inventory Management**
- Real-time stock tracking
- Transaction history for audit purposes
- Low stock alerts via reorder levels

### 5. **Workload Management**
- Technician capacity tracking
- Assignment status monitoring
- Performance metrics

### 6. **Feedback System**
- Customer rating and review system
- Linked to specific bookings
- Quality assurance tracking

## Sample Data
The system includes a `DataInitializationService` that populates the database with:
- 6 predefined roles
- Sample users for each role (admin, managers, staff, customers)
- Common inventory items (engine parts, fluids, electrical components)
- Sample service bookings with various statuses
- Inventory transaction history
- Technician profiles with specializations

## Database Indexes
Recommended indexes for performance:
- `users.username` (unique)
- `users.email` (unique)
- `users.phone_number` (unique)
- `bookings.booking_number` (unique)
- `bookings.booking_date`
- `inventory_items.item_name`
- `inventory_transactions.date`
- `technician_assignments.assignment_date`

## Security Considerations
- Passwords are encrypted using Spring Security's PasswordEncoder
- User authentication via Spring Security
- Role-based authorization for different user types
- Soft delete prevents accidental data loss
- Audit trails for compliance and debugging
