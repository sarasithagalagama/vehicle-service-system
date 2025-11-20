# Vehicle Service System

A Spring Boot web application for managing a vehicle service centre. The app provides a web UI (Thymeleaf) and JSON APIs for:

- User authentication and role-based dashboards (Admin, Manager, Receptionist, Technician, Inventory Manager, Customer).
- Booking management (create/update/cancel bookings, slot availability, pricing, payments, refunds).
- Inventory management (items, stock, transactions, low-stock alerts, issue parts to technicians).
- Feedback system (customers submit reviews; managers can view and moderate).
- Technician assignment and workload management (manager tools to assign and monitor technicians).
- Demonstrations of design patterns used (Strategy, Singleton, Factory via Spring IoC).

## Screenshots

## Key Info

- Framework: Spring Boot (Java)
- Build: Maven (wrapper scripts `mvnw` / `mvnw.cmd` included)
- Templates: Thymeleaf under `src/main/resources/templates`
- Static: `src/main/resources/static`
- Main class: `com.vehicleservice.VehicleServiceSystemApplication`

## Prerequisites

- Java 21 (or compatible JDK)
- Git (optional)
- Maven or use the included wrapper

## Build & Run

From project root (Windows PowerShell):

```powershell
# Build (skip tests for faster turnaround)
.\mvnw.cmd clean package -DskipTests

# Run the packaged jar
java -jar target\*.jar

# Run directly during development
.\mvnw.cmd spring-boot:run
```

Default port: `8080` (modifiable in `src/main/resources/application.properties`).

## Implemented Features (detailed)

1. Authentication & Users

- Login: `GET /login` (login form)
- Register: `GET /register`, `POST /register` (server-side validation and uniqueness checks)
- Role-based redirect on `/` (redirects to dashboards depending on role)

2. Booking Module

- Customer booking flow (customer-facing):

  - `GET /customer/dashboard` — customer dashboard
  - `POST /customer/bookings/save` — create booking
  - `GET /customer/bookings/{id}` — view booking (JSON)
  - `POST /customer/bookings/{id}/update` — update booking
  - `POST /customer/bookings/{id}/cancel` — cancel booking
  - `GET /customer/slots/available` and `GET /customer/slots/check` — slot availability APIs
  - Payment endpoints: `POST /customer/payment/process`, `POST /customer/payment/calculate-fees`
  - Pricing: `POST /customer/pricing/calculate` (uses Strategy pattern)

- Staff-side booking management (receptionist/technician/manager):
  - `POST /staff/bookings` — create booking (staff)
  - `DELETE /staff/bookings/{id}` — delete booking
  - `GET /staff/bookings/{id}` — get booking (JSON)
  - `PUT /staff/bookings/{id}` — update booking (JSON)
  - `POST /staff/bookings/{id}/refund` — process refund
  - `POST /staff/bookings/{id}/cancel` — cancel with optional refund
  - Slot APIs: `GET /staff/slots/available`, `GET /staff/slots/week`, `GET /staff/slots/check`, `POST /staff/slots/refresh`
  - Pricing & fees: `GET /staff/service-pricing`, `POST /staff/pricing/calculate`, `POST /staff/payment/calculate-fees`, `POST /staff/calculate-cost`

3. Inventory Module

- Inventory manager dashboard: `GET /staff/inventory-manager/dashboard`
- Create/update items: `POST /staff/inventory/save`, `POST /staff/inventory/update`
- Delete item: `DELETE /staff/inventory/delete/{id}` (AJAX/JSON)
- Stock operations: `POST /staff/inventory/add-stock/{id}`, `POST /staff/inventory/issue-parts`
- AJAX endpoints: `GET /staff/inventory/item/{id}`, `GET /staff/inventory/low-stock`, `GET /staff/inventory/transactions`
- Features: low-stock alerts, validation on item fields, transaction history

4. Feedback Module

- Customer flows:

  - `GET /customer/bookings/{id}/feedback` — feedback form for a booking
  - `POST /customer/bookings/{id}/feedback` — submit feedback
  - `GET /customer/feedbacks` — list my feedbacks
  - Edit/delete flows (`/feedbacks/{id}/edit`, `/feedbacks/{id}/delete`)

- Manager flows (moderation):
  - `GET /feedbacks` — list all feedbacks (manager view)
  - `GET /feedbacks/{id}` — feedback detail (manager)
  - `POST /feedbacks` — create (admin/manager)
  - `POST /feedbacks/update/{id}` — update
  - `DELETE /feedbacks/delete/{id}` — delete

5. Manager & Technician Assignment

- Manager dashboard: `GET /manager/dashboard`
- Assign/unassign technicians: `POST /manager/assign-technician`, `POST /manager/unassign-technician`
- Create assignments: `POST /manager/assignments` and related endpoints
- Update status: `/manager/assignments/{id}/status`, `/manager/assignments/{id}/complete`
- Export assignments CSV: `GET /manager/assignments/export`
- Workload & stats endpoints: `/manager/technician-workload`, `/manager/technicians-with-assignments`, `/manager/feedback-stats`

6. Staff Dashboards

- Receptionist: `GET /staff/receptionist/dashboard` (create customers `POST /staff/receptionist/customers/create`)
- Technician: `GET /staff/technician/dashboard`
- Inventory manager: `GET /staff/inventory-manager/dashboard`

7. Design Pattern Demos & Utilities

- Strategy (pricing): `GET /test/strategy/{serviceType}` and `POST /pricing/calculate`
- Singleton verification and demo endpoints:
  - `GET /test/singleton-verification`
  - `GET /test/singleton-bean/{beanName}`
  - `GET /test/singleton-manager`
  - `GET /test/factory-beans` (inspects Spring context)
  - Booking-controller demo: `GET /demo/singleton-verification` (returns verification stats)

## Templates & Static

- Templates organized by role and feature: `templates/admin/`, `templates/auth/`, `templates/customer/`, `templates/manager/`, `templates/staff/`, `templates/feedback/`.
- Static CSS at `src/main/resources/static/css` (e.g. `dashboard.css`, `landing.css`).

## Configuration

- Main configuration: `src/main/resources/application.properties`.
- Security configuration is in `com.vehicleservice.config.SecurityConfig`.

## Database & Sample Data

- Project expects a relational DB configured via properties. If you want a developer-friendly H2 profile and sample seed data, I can add `application-dev.properties` and `data.sql`.

## Tests

Run unit/integration tests:

```powershell
.\mvnw.cmd test
```

## Next steps (optional)

- Add `application-dev.properties` + H2 in-memory profile and `data.sql` seed file for quick local testing.
- Add a `Dockerfile` and `docker-compose.yml` for local development (with H2 or an external DB).
- Generate an OpenAPI/Swagger spec for the JSON APIs.

If you want any of the next steps, tell me which (e.g. "add H2 dev profile and data.sql" or "add Dockerfiles") and I will implement them.

## License

No license file provided — add a `LICENSE` if you want to declare one.
