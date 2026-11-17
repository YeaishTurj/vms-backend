# Vehicle Management System (VMS) — Backend Service (`vms-backend`)

Enterprise REST API backend service for the Vehicle Management System (VMS), built with **Java 17**, **Spring Boot 3.4**, **Spring Security (stateless JWT)**, and **Spring Data JPA** with **PostgreSQL** relational persistence.

---

## 1. Architectural Role & Repository Isolation

The Vehicle Management System is engineered as a decoupled, multi-repository architecture:

```
┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│          vms-frontend           │           │           vms-backend           │
│       (Flutter Web / Dart)      │   HTTP    │     (Java / Spring Boot / SQL)   │
│  • Material 3 Enterprise UI     │ ◄───────► │  • RESTful API Endpoints        │
│  • Desktop-First Layout         │   REST    │  • Spring Security (JWT)        │
│  • Independent Repository       │   JSON    │  • Business Rules & Validation  │
│                                 │           │  • PostgreSQL SQL Persistence   │
└─────────────────────────────────┘           └─────────────────────────────────┘
```

> [!IMPORTANT]
> **Strict Separation of Concerns:**
> - `vms-backend` is dedicated exclusively to server-side business logic, security, REST APIs, and database persistence.
> - Zero frontend, Flutter, or Dart code resides in this repository.
> - The frontend is maintained as a completely independent repository (`vms-frontend`).

---

## 2. Technology Stack

| Layer | Component | Version / Specification |
|---|---|---|
| **Language & Runtime** | Java | Java 17 LTS |
| **Application Framework** | Spring Boot | 3.4.3 (Spring Framework 6.2) |
| **Security & Auth** | Spring Security & JJWT | Stateless JWT (Bearer token), BCrypt password hashing, JJWT 0.12.6 |
| **Persistence / ORM** | Spring Data JPA | Hibernate 6.6, Spring Data Repositories |
| **Primary Database** | PostgreSQL | PostgreSQL 16 (production/local runtime) |
| **Test Database** | In-Memory H2 | Isolated H2 database for zero-impact test execution |
| **Validation** | Jakarta Bean Validation | Hibernate Validator (`@NotBlank`, `@Size`, `@Valid`) |
| **Build & Packaging** | Apache Maven | Maven Wrapper (`./mvnw`) |
| **Testing** | Testing Suite | JUnit 5, Mockito, AssertJ, Spring Boot Test, Spring Security Test (79 tests) |

---

## 3. Database & Environment Configuration

The backend uses environment variables for all sensitive and deployment-specific settings with zero hardcoded credentials:

| Environment Variable | Description | Default Fallback |
|---|---|---|
| `DB_URL` / `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/vms_db` |
| `DB_USERNAME` / `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | Database password | *(None / required from environment)* |
| `PORT` | HTTP server listening port | `8081` |
| `JWT_SECRET` | Cryptographically secure 256-bit signing key | Default development secret key |
| `JWT_EXPIRATION_MS` | JWT token validity window in milliseconds | `86400000` (24 hours) |

An environment template is provided at [`.env.example`](file:///home/yeaish/repos/vms-backend/.env.example). Local `.env` files are strictly excluded from version control via [`.gitignore`](file:///home/yeaish/repos/vms-backend/.gitignore).

---

## 4. Setup & Running Locally

### Prerequisites:
- Java 17 JDK or higher installed
- PostgreSQL 16 service running (e.g. Docker container or local service)
- Database created: `CREATE DATABASE vms_db;`

### Step 1: Configure Environment Variables
Copy the template and provide your database credentials:
```bash
cd /home/yeaish/repos/vms-backend
cp .env.example .env
# Edit .env with your PostgreSQL credentials
```

### Step 2: Build & Verify
Run clean verification to compile the project and execute the complete test suite:
```bash
./mvnw clean verify
```

### Step 3: Run the Application

**Option A: Using Maven Wrapper**
```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

**Option B: Running the Packaged Executable JAR**
```bash
set -a && source .env && set +a
java -jar target/vms-backend-0.0.1-SNAPSHOT.jar
```
The server will start listening at: `http://localhost:8081`

---

## 5. REST API Specification

### 5.1. Authentication Endpoints (Public)

| Method | Endpoint | Description | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/auth/login` | Authenticate with credentials and receive JWT | `{"username": "admin", "password": "..."}` | `200 OK` + `{"token": "...", "type": "Bearer", "username": "admin", ...}` |
| `GET` | `/auth/me` | Fetch authenticated user profile | Bearer Token header | `200 OK` + User details |
| `GET` | `/api/v1/health` | Health check endpoint | None | `200 OK` + `{"status": "UP"}` |

### 5.2. Vehicle Fleet Management Endpoints (Protected by JWT)
*Endpoints support both `/vehicles` and `/api/v1/vehicles` mappings.*

| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| `GET` | `/vehicles?page=0&size=10&sort=id,asc` | Paginated vehicle list with Spring Page JSON metadata | `200 OK`, `400 Bad Request`, `401 Unauthorized` |
| `GET` | `/vehicles/{id}` | Retrieve single vehicle by ID | `200 OK`, `401 Unauthorized`, `404 Not Found` |
| `POST` | `/vehicles` | Register a new vehicle | `201 Created` + `Location`, `400 Bad Request`, `401 Unauthorized`, `409 Conflict` |
| `PUT` | `/vehicles/{id}` | Update existing vehicle attributes | `200 OK`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/vehicles/{id}` | Delete vehicle by ID | `204 No Content`, `401 Unauthorized`, `404 Not Found` |

### 5.3. Vehicle Data Model & Validation Rules

```json
{
  "vehicleNumber": "ABC-1234",
  "company": "Pacific Freight Inc",
  "type": "Semi-Truck",
  "brand": "Freightliner",
  "model": "Cascadia",
  "status": "Active"
}
```

- **Uniqueness Rule:** `vehicleNumber` must be unique across all vehicles (case-insensitive and trimmed).
- **Service Layer Guard:** Rejects duplicates with `409 Conflict` and descriptive error message.
- **Database Schema Guard:** `uk_vehicles_vehicle_number` unique constraint prevents race conditions.
- **Update Safety:** A vehicle retains its own vehicle number during updates without triggering a duplicate error.

---

## 6. Project Architecture

```
vms-backend/
├── pom.xml                                      # Maven configuration (Java 17, Spring Boot 3.4.3)
├── mvnw / mvnw.cmd                              # Maven Wrapper
├── .gitignore                                   # Ignore targets, logs, .env files
├── .env.example                                 # Template environment file
├── README.md                                    # Backend documentation
└── src/
    ├── main/
    │   ├── java/com/vms/
    │   │   ├── VmsBackendApplication.java       # Spring Boot main class
    │   │   ├── config/
    │   │   │   ├── CorsConfig.java              # CORS configuration for Flutter Web
    │   │   │   └── DataInitializer.java         # Initial user seeding with BCrypt
    │   │   ├── controller/
    │   │   │   ├── AuthController.java          # Login and user profile endpoints
    │   │   │   ├── DatabaseHealthController.java# Database health check
    │   │   │   └── VehicleController.java       # Vehicle CRUD and pagination
    │   │   ├── dto/
    │   │   │   ├── AuthResponse.java            # JWT auth token DTO
    │   │   │   ├── LoginRequest.java            # Login credentials DTO
    │   │   │   ├── UserResponse.java            # User profile DTO
    │   │   │   ├── VehicleRequest.java          # Validated vehicle input DTO
    │   │   │   └── VehicleResponse.java         # Safe vehicle response DTO
    │   │   ├── entity/
    │   │   │   ├── User.java                    # System user entity (BCrypt passwords)
    │   │   │   └── Vehicle.java                 # Vehicle JPA entity with unique constraint
    │   │   ├── exception/
    │   │   │   ├── DuplicateVehicleNumberException.java
    │   │   │   ├── GlobalExceptionHandler.java  # Centralized REST error mapping
    │   │   │   └── ResourceNotFoundException.java
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java          # Spring Data JPA User repository
    │   │   │   └── VehicleRepository.java       # Spring Data JPA Vehicle repository
    │   │   ├── security/
    │   │   │   ├── CustomUserDetailsService.java# UserDetails loader
    │   │   │   ├── JwtAccessDeniedHandler.java  # 403 Forbidden handler
    │   │   │   ├── JwtAuthenticationEntryPoint.java # 401 Unauthorized handler
    │   │   │   ├── JwtAuthenticationFilter.java # OncePerRequestFilter JWT validator
    │   │   │   ├── JwtTokenProvider.java        # JJWT token signer and parser
    │   │   │   └── SecurityConfig.java          # Security filter chain and authorization rules
    │   │   └── service/
    │   │       ├── AuthService.java             # User authentication and JWT issuance
    │   │       └── VehicleService.java          # Vehicle business logic and validation
    │   └── resources/
    │       └── application.yml                  # Base configuration with environment variables
    └── test/
        ├── java/com/vms/
        │   ├── VmsBackendApplicationTests.java  # Context load test
        │   ├── controller/
        │   │   ├── AuthControllerTest.java      # Auth controller WebMvc tests
        │   │   ├── DatabaseHealthControllerTest.java
        │   │   ├── VehicleControllerTest.java   # Vehicle controller WebMvc tests
        │   │   └── VehicleRestApiIntegrationTest.java # Full end-to-end integration test
        │   ├── repository/
        │   │   └── VehicleRepositoryTest.java   # DataJpa tests with unique constraints
        │   ├── security/
        │   │   ├── JwtTokenProviderTest.java    # JWT signer and parser unit tests
        │   │   ├── PasswordEncoderSecurityTest.java # BCrypt hashing security tests
        │   │   └── SecurityIntegrationTest.java # Spring Security end-to-end tests
        │   └── service/
        │       ├── VehicleDuplicateRuleUnitTest.java # Isolated duplicate rule unit tests
        │       └── VehicleServiceTest.java      # Service layer mock unit tests
        └── resources/
            └── application.yml                  # Isolated H2 test configuration
```

---

## 7. Quality Assurance & Test Coverage

The test suite covers all application layers with **79 automated tests**:

```bash
# Run complete test suite
./mvnw test

# Run focused integration test
./mvnw test -Dtest=VehicleRestApiIntegrationTest
```

- **Unit Tests:** Business logic, uniqueness checks, DTO mapping, password hashing, and JWT token lifecycle.
- **Repository Tests:** Spring Data JPA queries, casing insensitivity, and SQL unique constraint violations.
- **Web Layer Tests:** Endpoint routing, validation error formatting, and HTTP status codes.
- **Security Integration Tests:** Authentication lifecycle, unauthenticated rejection (401), invalid tokens, and access denial.
- **End-to-End REST API Integration Tests:** Full HTTP request to SQL database persistence for CREATE, READ (paginated and single), UPDATE, DELETE, DUPLICATE (409), VALIDATION (400), and NOT FOUND (404).
