# VMS Vehicle REST API Documentation

Base Paths: `/vehicles` and `/api/v1/vehicles`

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
