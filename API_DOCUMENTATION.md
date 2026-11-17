# VMS REST API & Authentication Documentation

Base URL: `http://localhost:8081`  
Endpoints: `/auth`, `/api/v1/auth`, `/vehicles`, `/api/v1/vehicles`, `/api/v1/health`

---

## Authentication & Security Overview

The backend uses **Spring Security 6** with **stateless JWT (JSON Web Token)** authentication using HMAC-SHA512/HMAC-SHA256 signature verification.
- **Passwords:** Stored exclusively as salted hashes using `BCryptPasswordEncoder`. Passwords are never stored or logged in plain text and are excluded from all API response payloads (`@JsonIgnore`).
- **Authorization:** Vehicle fleet endpoints (`/vehicles/**` and `/api/v1/vehicles/**`) require an `Authorization: Bearer <token>` header.
- **Public Endpoints:** Authentication endpoints (`/auth/login`, `/api/v1/auth/login`), CORS preflight (`OPTIONS /**`), and health checks (`/api/v1/health`) are publicly accessible without tokens.
- **Default System Accounts (Seeded via `DataInitializer`):**
  - **Administrator:** `username: admin`, `password: admin123` (`ROLE_ADMIN`)
  - **Standard User:** `username: user`, `password: user123` (`ROLE_USER`)

---

## Authentication Endpoints

### 1. User Login

- **Method:** `POST`
- **Paths:** `/auth/login` or `/api/v1/auth/login`
- **Authentication:** None (Public)
- **Headers:** `Content-Type: application/json`
- **Status Codes:**
  - `200 OK`: Successful authentication, JWT issued.
  - `400 Bad Request`: Validation failure (blank username/password).
  - `401 Unauthorized`: Invalid username or password.

#### Request Body
```json
{
  "username": "admin",
  "password": "admin123"
}
```

#### Response (`200 OK`)
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNzk0OTMzMDUyLCJleHAiOjE3OTUwMTk0NTJ9...",
  "type": "Bearer",
  "expiresIn": 86400,
  "username": "admin",
  "roles": [
    "ROLE_ADMIN"
  ]
}
```

#### Error Response (`401 Unauthorized`)
```json
{
  "timestamp": "2026-11-17T16:30:31.563Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password"
}
```

---

### 2. Current User Profile

- **Method:** `GET`
- **Paths:** `/auth/me` or `/api/v1/auth/me`
- **Authentication:** `Bearer <JWT_TOKEN>`
- **Status Codes:**
  - `200 OK`: Returns authenticated user's profile metadata.
  - `401 Unauthorized`: Missing, expired, or invalid token.

#### Response (`200 OK`)
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@vms.corp",
  "role": "ROLE_ADMIN"
}
```

---

## Protected Fleet Management Endpoints

All endpoints below require the following HTTP header:
```http
Authorization: Bearer <JWT_TOKEN>
```
If this header is missing or contains an invalid/expired token, the server returns:
```json
{
  "path": "/vehicles",
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "timestamp": "2026-11-17T16:30:16.255Z",
  "status": 401
}
```

---

## 1. List Vehicles (Paginated)

- **Method:** `GET`
- **Path:** `/vehicles`
- **Query Parameters:**
  - `page` (integer, default: 0): Zero-based page number.
  - `size` (integer, default: 10, max: 100): Number of records per page.
  - `sort` (string, default: "id,asc"): Format: `<field>,<asc|desc>`.
- **Status Codes:**
  - `200 OK`: Successful retrieval.
  - `400 Bad Request`: `page < 0`, `size < 1`, or `size > 100`.

### Example Request
```http
GET /vehicles?page=0&size=10 HTTP/1.1
Host: localhost:8081
Accept: application/json
```

### Example Response (`200 OK`)
```json
{
  "content": [
    {
      "id": 1,
      "vehicleNumber": "ABC-1234",
      "company": "Apex Logistics",
      "type": "Semi-Truck",
      "brand": "Freightliner",
      "model": "Cascadia",
      "status": "Active"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

---

## 2. Get Vehicle by ID

- **Method:** `GET`
- **Path:** `/vehicles/{id}`
- **Path Parameters:**
  - `id` (integer/long): Vehicle unique identifier.
- **Status Codes:**
  - `200 OK`: Vehicle found.
  - `404 Not Found`: No vehicle exists with the given ID.

### Example Request
```http
GET /vehicles/1 HTTP/1.1
Host: localhost:8081
Accept: application/json
```

### Example Response (`200 OK`)
```json
{
  "id": 1,
  "vehicleNumber": "ABC-1234",
  "company": "Apex Logistics",
  "type": "Semi-Truck",
  "brand": "Freightliner",
  "model": "Cascadia",
  "status": "Active"
}
```

### Error Response (`404 Not Found`)
```json
{
  "timestamp": "2026-11-17T15:40:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Vehicle not found with ID: 999"
}
```

---

## 3. Create Vehicle

- **Method:** `POST`
- **Path:** `/vehicles`
- **Headers:** `Content-Type: application/json`
- **Status Codes:**
  - `201 Created`: Vehicle successfully created. Returns `Location` header.
  - `400 Bad Request`: Validation failure on required fields.
  - `409 Conflict`: Vehicle number is already registered.

### Example Request Body
```json
{
  "vehicleNumber": "ABC-1234",
  "company": "Apex Logistics",
  "type": "Semi-Truck",
  "brand": "Freightliner",
  "model": "Cascadia",
  "status": "Active"
}
```

### Example Response (`201 Created`)
```json
{
  "id": 1,
  "vehicleNumber": "ABC-1234",
  "company": "Apex Logistics",
  "type": "Semi-Truck",
  "brand": "Freightliner",
  "model": "Cascadia",
  "status": "Active"
}
```

### Error Response (`409 Conflict`)
```json
{
  "timestamp": "2026-11-17T15:40:00Z",
  "status": 409,
  "error": "Duplicate Vehicle Number",
  "message": "Vehicle number 'ABC-1234' is already registered."
}
```

### Error Response (`400 Bad Request`)
```json
{
  "timestamp": "2026-11-17T15:40:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Validation failed for one or more fields",
  "details": {
    "vehicleNumber": "Vehicle number is required",
    "company": "Company is required"
  }
}
```

---

## 4. Update Vehicle

- **Method:** `PUT`
- **Path:** `/vehicles/{id}`
- **Headers:** `Content-Type: application/json`
- **Status Codes:**
  - `200 OK`: Vehicle successfully updated.
  - `400 Bad Request`: Validation failure on request body.
  - `404 Not Found`: Vehicle with specified ID does not exist.
  - `409 Conflict`: Vehicle number is already used by another vehicle.

### Example Request Body
```json
{
  "vehicleNumber": "ABC-1234",
  "company": "Apex Global Logistics",
  "type": "Semi-Truck",
  "brand": "Freightliner",
  "model": "Cascadia Evolution",
  "status": "Under Maintenance"
}
```

### Example Response (`200 OK`)
```json
{
  "id": 1,
  "vehicleNumber": "ABC-1234",
  "company": "Apex Global Logistics",
  "type": "Semi-Truck",
  "brand": "Freightliner",
  "model": "Cascadia Evolution",
  "status": "Under Maintenance"
}
```

---

## 5. Delete Vehicle

- **Method:** `DELETE`
- **Path:** `/vehicles/{id}`
- **Status Codes:**
  - `204 No Content`: Vehicle successfully deleted.
  - `404 Not Found`: Vehicle with specified ID does not exist.

### Example Request
```http
DELETE /vehicles/1 HTTP/1.1
Host: localhost:8081
```

### Example Response
HTTP/1.1 204 No Content
