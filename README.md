# Vehicle Service Management System

A comprehensive web-based system for managing vehicle service operations, inventory, and customer feedback.

## 👥 Team Members & Responsibilities

| Team Member | Student ID | Responsibility | Primary Components |
|-------------|------------|----------------|-------------------|
| **Ahamed M. M. S** | - | Manage User Accounts | AdminController, AuthController, User Management |
| **Galagama S. T** | IT24100548 | Manage Vehicle Service Bookings | BookingController, CustomerController (booking parts) |
| **Udayanga D. A. D. N** | IT24100425 | Inventory Management | InventoryController, Inventory Management |
| **De Zoysa T. N. D** | IT24100387 | Submit Feedback | FeedbackController, CustomerController (feedback parts) |
| **De Silva T. R. S** | IT24100391 | Assign Technicians to Service Tasks | ManagerController, StaffController (technician parts) |

## 🏗️ Project Architecture

### Folder Structure & Responsibilities

```
src/main/java/com/vehicleservice/
├── controller/          # Web Interface Layer - Handle HTTP requests
│   ├── AdminController.java          → Ahamed (User Management)
│   ├── AuthController.java           → Ahamed (Authentication)
│   ├── BookingController.java        → Galagama (Bookings)
│   ├── CustomerController.java       → Galagama (Customer Bookings)
│   ├── FeedbackController.java       → De Zoysa (Feedback)
│   ├── InventoryController.java      → Udayanga (Inventory)
│   ├── ManagerController.java        → De Silva (Technician Assignments)
│   └── StaffController.java          → De Silva (Staff Dashboard)
├── entity/              # Data Models - Database table structures
│   ├── User.java                     → Ahamed
│   ├── Role.java                     → Ahamed
│   ├── Booking.java                  → Galagama
│   ├── Feedback.java                 → De Zoysa
│   ├── InventoryItem.java            → Udayanga
│   ├── InventoryTransaction.java     → Udayanga
│   ├── Technician.java               → De Silva
│   └── TechnicianAssignment.java     → De Silva
├── repository/          # Data Access Layer - Database operations
├── service/             # Business Logic Layer - Business rules
└── config/              # Configuration - Security and settings

src/main/resources/
├── templates/           # HTML Templates - User interfaces
│   ├── admin/           → Ahamed (Admin pages)
│   ├── auth/            → Ahamed (Login/Register)
│   ├── customer/        → Galagama (Customer portal)
│   ├── feedback/        → De Zoysa (Feedback pages)
│   ├── manager/         → De Silva (Manager pages)
│   └── staff/           → De Silva (Staff dashboards)
├── static/              # Static Assets - CSS, JS, images
└── application.properties # Application configuration
```

## 🎯 CRUD Operations for Viva Demonstration

### 1. Ahamed M. M. S - User Account Management

#### **CREATE (C)**
- **Register new user**: `POST /register`
- **Admin create user**: `POST /admin/users`
- **Create new role**: Admin panel functionality

#### **READ (R)**
- **View all users**: `GET /admin/dashboard`
- **View user details**: `GET /admin/api/users/{id}`
- **Search users**: Admin dashboard search
- **View user by role**: Filter by role (ADMIN, CUSTOMER, etc.)

#### **UPDATE (U)**
- **Update user profile**: `POST /admin/users/{id}`
- **Change user role**: Admin panel
- **Update user status**: Toggle active/inactive
- **Change password**: User profile update

#### **DELETE (D)**
- **Delete user**: `POST /admin/users/{id}/delete`
- **Deactivate user**: `POST /admin/users/{id}/toggle-status`

**Demo Flow:**
```
1. Show user registration form
2. Login as admin
3. View user list
4. Edit a user's details
5. Change user role
6. Delete a test user
```

---

### 2. Galagama S. T - Vehicle Service Bookings

#### **CREATE (C)**
- **Create booking**: `POST /bookings/create`
- **Customer booking**: `POST /customer/bookings/save`
- **Staff create booking**: `POST /staff/bookings`

#### **READ (R)**
- **View all bookings**: `GET /bookings`
- **View booking details**: `GET /bookings/{id}`
- **Search bookings**: `GET /bookings/search`
- **Filter bookings**: By status, date, service type

#### **UPDATE (U)**
- **Update booking**: `POST /bookings/{id}/edit`
- **Update booking status**: `POST /bookings/{id}/status`
- **Customer update**: `POST /customer/bookings/{id}/update`

#### **DELETE (D)**
- **Delete booking**: `POST /bookings/{id}/delete`
- **Cancel booking**: `POST /customer/bookings/{id}/cancel`

**Demo Flow:**
```
1. Create a new booking as customer
2. Show booking list with filters
3. Edit booking details
4. Change booking status (PENDING → PAID)
5. Cancel a booking
```

---


---

### 4. Udayanga D. A. D. N - Inventory Management

#### **CREATE (C)**
- **Add inventory item**: `POST /staff/inventory/save`
- **Add stock**: `POST /staff/inventory/add-stock/{id}`
- **Issue parts**: `POST /staff/inventory/issue-parts`

#### **READ (R)**
- **View inventory**: Inventory manager dashboard
- **View low stock**: `GET /staff/inventory/low-stock`
- **View transactions**: `GET /staff/inventory/transactions`
- **Search items**: By name, category

#### **UPDATE (U)**
- **Update inventory item**: `POST /staff/inventory/update`
- **Update stock levels**: Add/remove stock
- **Update item details**: Price, reorder level

#### **DELETE (D)**
- **Delete inventory item**: `GET /staff/inventory/delete/{id}`

**Demo Flow:**
```
1. Add new inventory item
2. Show inventory list with low stock alerts
3. Add stock to existing item
4. Issue parts to technician
5. Update item details
6. Delete test item
```

---

### 5. De Zoysa T. N. D - Feedback System

#### **CREATE (C)**
- **Submit feedback**: `POST /customer/bookings/{id}/feedback`
- **Create feedback**: `POST /feedbacks`

#### **READ (R)**
- **View all feedback**: `GET /feedbacks`
- **View customer feedback**: `GET /customer/feedbacks`
- **View feedback details**: `GET /feedbacks/{id}`
- **Manager view**: `GET /manager/feedbacks`

#### **UPDATE (U)**
- **Edit feedback**: `POST /customer/feedbacks/{id}/edit`
- **Update feedback**: `POST /feedbacks/update/{id}`

#### **DELETE (D)**
- **Delete feedback**: `POST /customer/feedbacks/{id}/delete`
- **Manager delete**: `POST /manager/feedbacks/{id}/delete`

**Demo Flow:**
```
1. Submit feedback for completed booking
2. Show feedback list (customer view)
3. Edit feedback rating/comment
4. Show manager feedback view
5. Delete test feedback
```

---

### 6. De Silva T. R. S - Technician Assignments

#### **CREATE (C)**
- **Assign technician**: `POST /manager/assignments/assign`
- **Create assignment**: `POST /manager/assignments`

#### **READ (R)**
- **View assignments**: `GET /manager/assignments`
- **View technician workload**: `GET /manager/technician-workload`
- **View assignment details**: `GET /manager/assignments/{id}`

#### **UPDATE (U)**
- **Update assignment**: `POST /manager/assignments/{id}/update`
- **Update status**: `POST /manager/assignments/{id}/status`
- **Complete assignment**: `POST /manager/assignments/{id}/complete`

#### **DELETE (D)**
- **Remove assignment**: `DELETE /manager/assignments/{id}`
- **Unassign technician**: `POST /manager/unassign-technician`

**Demo Flow:**
```
1. Assign technician to booking
2. Show assignment list
3. Update assignment status
4. Show technician workload
5. Complete assignment
6. Remove assignment
```

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven 3.6+
- SQL Server Database
- Spring Boot 3.5.5

### Installation
1. Clone the repository
2. Configure database connection in `application.properties`
3. Run `mvn clean install`
4. Start the application with `mvn spring-boot:run`
5. Access the application at `http://localhost:8080`

### Default Login Credentials
- **Admin**: admin/admin123
- **Manager**: manager/manager123
- **Receptionist**: receptionist/receptionist123
- **Technician**: technician/technician123
- **Inventory Manager**: inventory/inventory123

## 🔧 Technology Stack

- **Backend**: Spring Boot, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Database**: Microsoft SQL Server
- **Build Tool**: Maven
- **Security**: Spring Security with role-based access control

## 📋 Features

### User Management
- User registration and authentication
- Role-based access control (Admin, Manager, Staff, Customer)
- User profile management
- Password management

### Booking Management
- Service appointment scheduling
- Booking status tracking
- Slot availability management
- Customer self-service portal


### Inventory Management
- Parts and inventory tracking
- Low stock alerts
- Stock transactions
- Parts issuance to technicians

### Feedback System
- Customer feedback submission
- Rating system (1-5 stars)
- Feedback management
- Manager review interface

### Technician Assignment
- Work assignment management
- Workload balancing
- Assignment status tracking
- Performance monitoring

## 🎯 Viva Preparation Tips

### For Each Team Member:
1. **Prepare Demo Data**: Have test data ready for each operation
2. **Know the URLs**: Memorize the key endpoints for your component
3. **Show Error Handling**: Demonstrate validation and error messages
4. **Explain Business Logic**: Why each operation is important
5. **Show Integration**: How your component connects with others

### Demo Script Template:
```
1. "I'll show you the CREATE operation..."
2. "Now let me demonstrate the READ functionality..."
3. "Here's how UPDATE works..."
4. "Finally, the DELETE operation..."
5. "This integrates with [other team member's] component because..."
```

### Key Points to Mention:
- **Security**: Role-based access control
- **Validation**: Input validation and error handling
- **User Experience**: Intuitive interfaces
- **Integration**: How components work together
- **Database**: How data is stored and retrieved

## 📞 Team Communication

- **Daily Standup**: Each member reports progress on their component
- **Integration Points**: Regular sync on interfaces between components
- **Code Reviews**: Cross-review each other's work
- **Testing**: Integration testing as features are completed

## 🔄 Integration Points

### Cross-Dependencies:
1. **Ahamed ↔ Galagama**: User accounts needed for booking customers
2. **Galagama ↔ De Silva**: Bookings need technician assignments
3. **De Silva ↔ Udayanga**: Technicians need parts from inventory
4. **Galagama ↔ De Zoysa**: Completed bookings can receive feedback

## 📈 Development Timeline

### Phase 1: Foundation (Week 1-2)
- Ahamed: User authentication and management
- Udayanga: Inventory system foundation
- De Silva: Technician management

### Phase 2: Core Features (Week 3-4)
- Galagama: Booking system
- De Zoysa: Feedback system

### Phase 3: Integration (Week 5-6)
- All members: System integration
- Testing and bug fixes
- Final deployment

## 📝 License

This project is developed for educational purposes as part of a university assignment.

## 👨‍💻 Contributors

- **Ahamed M. M. S** - User Account Management
- **Galagama S. T** (IT24100548) - Vehicle Service Bookings
- **Udayanga D. A. D. N** (IT24100425) - Inventory Management
- **De Zoysa T. N. D** (IT24100387) - Feedback System
- **De Silva T. R. S** (IT24100391) - Technician Assignments

---

**Note**: Each team member should be able to demonstrate their CRUD operations in **5-10 minutes** during the viva!
