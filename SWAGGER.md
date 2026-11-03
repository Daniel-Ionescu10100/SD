# Energy Management System — Swagger / OpenAPI Specification

**Institution:** Technical University of Cluj-Napoca (UTCN)  
**Course:** Distributed Systems (Assignment 1)  
**Evaluation Requirement:** *Swagger documentation (path, HTTP method, short description, input params, response)*  
**Specification Version:** OpenAPI 3.0.3  
**Interactive UI:** Available via [`swagger-ui.html`](swagger-ui.html) or online at `https://localhost/docs` when containers are running.

---

## 1. Quick Reference Endpoint Table

| # | HTTP Method | Path | Short Description | Input Parameters | Response |
|---|---|---|---|---|---|
| **1** | `POST` | `/auth/login` | Authenticate user credentials and issue cryptographically signed JWT token. | **Body (JSON):** `username` (string, req), `password` (string, req) | **200 OK:** `{ token, username, role, userId }`<br>**400:** Validation error<br>**401:** Invalid credentials |
| **2** | `POST` | `/auth/register` | Register user credentials directly into auth database. | **Body (JSON):** `username` (string, req), `password` (string, req), `role` (ADMIN\|CLIENT, req) | **201 Created:** UUID (Credential ID)<br>**409:** Username already exists |
| **3** | `GET` | `/auth/validate` | Internal Nginx sub-request to validate JWT signature & expiration. | **Header:** `Authorization: Bearer <token>` | **200 OK:** Headers: `X-User-Id`, `X-Username`, `X-Role`<br>**401:** Token invalid/expired |
| **4** | `DELETE` | `/auth/user/{username}` | Internal synchronization endpoint to de-provision credentials on user deletion. | **Path Param:** `username` (string, req) | **204 No Content** |
| **5** | `GET` | `/user` | List all registered user accounts (ADMIN role only). | **Header:** `Authorization: Bearer <token>` | **200 OK:** Array of UserDTO `{ id, name, username, role }`<br>**403:** Forbidden |
| **6** | `GET` | `/user/{id}` | Retrieve specific user profile. | **Header:** `Authorization: Bearer <token>`<br>**Path Param:** `id` (UUID, req) | **200 OK:** UserDTO `{ id, name, username, role }`<br>**403:** Client forbidden if `id != ownId`<br>**404:** Not Found |
| **7** | `POST` | `/user` | Create user profile with strong consistency (syncs to Auth & Device DBs). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Body (JSON):** UserDetailsDTO `{ name, username, password, role }` | **201 Created:** UUID (User ID)<br>**400:** Bad Request<br>**403:** Forbidden<br>**409:** Duplicate username |
| **8** | `PUT` | `/user/{id}` | Update user details (syncs updated name/username to Device DB). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Path Param:** `id` (UUID, req)<br>**Body (JSON):** UserDetailsDTO `{ name, username, password, role }` | **200 OK:** UserDTO<br>**403:** Forbidden<br>**404:** User not found |
| **9** | `DELETE` | `/user/{id}` | Delete user (unassigns and deletes shadow person in Device DB, revokes Auth credentials). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Path Param:** `id` (UUID, req) | **204 No Content**<br>**403:** Forbidden<br>**404:** Not Found |
| **10** | `GET` | `/device` | Retrieve catalog of all smart metering devices and owners (ADMIN role only). | **Header:** `Authorization: Bearer <token>` (ADMIN) | **200 OK:** Array of DeviceDTO `{ id, name, description, maxConsumption, userId }`<br>**403:** Forbidden |
| **11** | `GET` | `/device/{id}` | Retrieve specific smart metering device details (ADMIN role only). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Path Param:** `id` (UUID, req) | **200 OK:** DeviceDTO `{ id, name, description, maxConsumption, userId }`<br>**404:** Not Found |
| **12** | `GET` | `/device/user/{userId}` | Retrieve devices assigned to a specific user account. | **Header:** `Authorization: Bearer <token>`<br>**Path Param:** `userId` (UUID, req) | **200 OK:** Array of DeviceDTO<br>**403:** Client forbidden if `userId != ownId` |
| **13** | `POST` | `/device` | Register a new smart meter device entity and link to user (ADMIN role only). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Body (JSON):** DeviceDetailsDTO `{ name, description, maxConsumption, userId }` | **201 Created:** UUID (Device ID)<br>**400:** Bad Request<br>**404:** Person not found |
| **14** | `PUT` | `/device/{id}` | Update device configuration, max consumption, or user assignment (ADMIN role only). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Path Param:** `id` (UUID, req)<br>**Body (JSON):** DeviceDetailsDTO `{ name, description, maxConsumption, userId }` | **200 OK:** DeviceDTO<br>**404:** Device or Person not found |
| **15** | `DELETE` | `/device/{id}` | Delete smart meter device from inventory (ADMIN role only). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Path Param:** `id` (UUID, req) | **204 No Content**<br>**404:** Not Found |
| **16** | `PATCH` | `/device/{deviceId}/assign/{userId}` | Associate an existing device with a specific client user (ADMIN role only). | **Header:** `Authorization: Bearer <token>` (ADMIN)<br>**Path Params:** `deviceId` (UUID, req), `userId` (UUID, req) | **200 OK:** Updated DeviceDTO<br>**404:** Device or User not found |
| **17** | `DELETE` | `/device/user/{userId}` | Internal synchronization endpoint to disassociate all devices from user. | **Path Param:** `userId` (UUID, req) | **204 No Content** |
| **18** | `POST` | `/person` | Internal synchronization endpoint to insert shadow user into Device DB. | **Body (JSON):** PersonDTO `{ id, name, username }` | **201 Created:** UUID |
| **19** | `PUT` | `/person/{id}` | Internal synchronization endpoint to update shadow user in Device DB. | **Path Param:** `id` (UUID, req)<br>**Body (JSON):** PersonDTO `{ id, name, username }` | **200 OK:** PersonDTO |
| **20** | `DELETE` | `/person/{id}` | Internal synchronization endpoint to remove shadow user and clear FK device links. | **Path Param:** `id` (UUID, req) | **204 No Content** |

---

## 2. Detailed Endpoint Specifications

### 2.1 Authentication Microservice (`auth-service`)

#### `POST /auth/login`
- **Short Description:** Authenticates user credentials via BCrypt salted hashes in `auth_db` and generates a cryptographically signed JWT bearer token containing role and userId claims.
- **Input Parameters:**
  - **Content-Type:** `application/json`
  - **Body Schema (`LoginRequestDTO`):**
    ```json
    {
      "username": "admin",
      "password": "admin123"
    }
    ```
- **Responses:**
  - `200 OK`:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsInVzZXJJZCI6IjExMTExMTExLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMSJ9...",
      "username": "admin",
      "role": "ADMIN",
      "userId": "11111111-1111-1111-1111-111111111111"
    }
    ```
  - `401 Unauthorized`:
    ```json
    {
      "error": "Unauthorized",
      "message": "Invalid username or password"
    }
    ```

#### `POST /auth/register`
- **Short Description:** Ingests user credentials, hashes password with BCrypt (factor 10), and stores in `auth_db`.
- **Input Parameters:**
  - **Body Schema (`RegisterRequestDTO`):**
    ```json
    {
      "username": "client2",
      "password": "password123",
      "role": "CLIENT"
    }
    ```
- **Responses:**
  - `201 Created`: `"4a2b9f10-1234-5678-9abc-def012345678"` (UUID)
  - `409 Conflict`: Username already taken.

#### `GET /auth/validate`
- **Short Description:** Internal validation endpoint called by Nginx `auth_request` gateway directive. Parses and verifies JWT signature and expiration.
- **Input Parameters:**
  - **Headers:** `Authorization: Bearer <token>`
- **Responses:**
  - `200 OK`: Emits headers `X-User-Id`, `X-Username`, `X-Role` for downstream reverse proxy forwarding.
  - `401 Unauthorized`: Missing or invalid token.

---

### 2.2 User Management Microservice (`user-service`)

#### `GET /user`
- **Short Description:** Retrieves the full list of registered users. Requires `ADMIN` role.
- **Input Parameters:**
  - **Headers:** `Authorization: Bearer <token>` (injected `X-Role: ADMIN`)
- **Responses:**
  - `200 OK`:
    ```json
    [
      {
        "id": "11111111-1111-1111-1111-111111111111",
        "name": "System Administrator",
        "username": "admin",
        "role": "ADMIN"
      },
      {
        "id": "22222222-2222-2222-2222-222222222222",
        "name": "John Doe",
        "username": "client1",
        "role": "CLIENT"
      }
    ]
    ```
  - `403 Forbidden`: Insufficient permissions.

#### `GET /user/{id}`
- **Short Description:** Retrieves a single user profile. `ADMIN` can retrieve any user; `CLIENT` can only access their own user matching `X-User-Id`.
- **Input Parameters:**
  - **Path Parameter:** `id` (UUID format, required)
  - **Headers:** `Authorization: Bearer <token>`
- **Responses:**
  - `200 OK`: `UserDTO` object.
  - `403 Forbidden`: Client attempted to view another user's profile.
  - `404 Not Found`: User not found in database.

#### `POST /user`
- **Short Description:** Creates a new user profile with strong transactional consistency across services. Syncs to `auth-service` (`/auth/register`) and `device-service` (`/person`). Requires `ADMIN` role.
- **Input Parameters:**
  - **Body Schema (`UserDetailsDTO`):**
    ```json
    {
      "name": "Alice Smith",
      "username": "alices",
      "password": "Password123!",
      "role": "CLIENT"
    }
    ```
- **Responses:**
  - `201 Created`: UUID of newly created user.
  - `400 Bad Request`: Field validation error (blank fields or invalid role).
  - `409 Conflict`: Username already taken.

#### `PUT /user/{id}`
- **Short Description:** Updates user profile attributes and propagates name/username updates to `device-service` shadow table. Requires `ADMIN` role.
- **Input Parameters:**
  - **Path Parameter:** `id` (UUID, required)
  - **Body Schema (`UserDetailsDTO`):** Complete user payload.
- **Responses:**
  - `200 OK`: Updated `UserDTO`.
  - `404 Not Found`: User UUID not found.

#### `DELETE /user/{id}`
- **Short Description:** Strong consistency user deletion: unassigns devices and deletes shadow person in `device-service`, revokes credentials in `auth-service`, and deletes user entity. Requires `ADMIN` role.
- **Input Parameters:**
  - **Path Parameter:** `id` (UUID, required)
- **Responses:**
  - `204 No Content`: Successful deletion.
  - `404 Not Found`: User not found.

---

### 2.3 Device Management Microservice (`device-service`)

#### `GET /device`
- **Short Description:** Retrieves list of all smart metering devices and their owner links. Requires `ADMIN` role.
- **Input Parameters:**
  - **Headers:** `Authorization: Bearer <token>` (injected `X-Role: ADMIN`)
- **Responses:**
  - `200 OK`:
    ```json
    [
      {
        "id": "33333333-3333-3333-3333-333333333331",
        "name": "Smart Meter Living Room",
        "description": "Monitors HVAC and lighting in main living area",
        "maxConsumption": 450.5,
        "userId": "22222222-2222-2222-2222-222222222222"
      },
      {
        "id": "33333333-3333-3333-3333-333333333333",
        "name": "Smart Meter Garage",
        "description": "Unassigned meter for electric vehicle charger",
        "maxConsumption": 1200.0,
        "userId": null
      }
    ]
    ```

#### `GET /device/{id}`
- **Short Description:** Retrieves device details by UUID. Requires `ADMIN` role.
- **Input Parameters:**
  - **Path Parameter:** `id` (UUID, required)
- **Responses:**
  - `200 OK`: `DeviceDTO` object.
  - `404 Not Found`: Device not found.

#### `GET /device/user/{userId}`
- **Short Description:** Retrieves all smart meters mapped to a user. `ADMIN` can query any user; `CLIENT` can only query their own `userId`.
- **Input Parameters:**
  - **Path Parameter:** `userId` (UUID, required)
  - **Headers:** `Authorization: Bearer <token>`
- **Responses:**
  - `200 OK`: Array of `DeviceDTO` assigned to this user.
  - `403 Forbidden`: Client queried another user's devices.

#### `POST /device`
- **Short Description:** Registers a new smart meter. If `userId` is supplied, validates that person exists in the shadow table and sets foreign key. Requires `ADMIN` role.
- **Input Parameters:**
  - **Body Schema (`DeviceDetailsDTO`):**
    ```json
    {
      "name": "Smart Meter Balcony",
      "description": "Monitors outdoor heater",
      "maxConsumption": 350.0,
      "userId": "22222222-2222-2222-2222-222222222222"
    }
    ```
- **Responses:**
  - `201 Created`: UUID of newly created device.
  - `400 Bad Request`: Validation failure.
  - `404 Not Found`: Provided `userId` does not exist in shadow person table.

#### `PUT /device/{id}`
- **Short Description:** Updates device details or reassigns user foreign key. Requires `ADMIN` role.
- **Input Parameters:**
  - **Path Parameter:** `id` (UUID, required)
  - **Body Schema (`DeviceDetailsDTO`):** Device update payload.
- **Responses:**
  - `200 OK`: Updated `DeviceDTO`.
  - `404 Not Found`: Device or target person not found.

#### `DELETE /device/{id}`
- **Short Description:** Deletes device from database. Requires `ADMIN` role.
- **Input Parameters:**
  - **Path Parameter:** `id` (UUID, required)
- **Responses:**
  - `204 No Content`: Device deleted.
  - `404 Not Found`: Device not found.

#### `PATCH /device/{deviceId}/assign/{userId}`
- **Short Description:** Reassigns or links an existing smart meter to a client user account. Enforces database foreign key constraint against `persons` table. Requires `ADMIN` role.
- **Input Parameters:**
  - **Path Parameters:**
    - `deviceId` (UUID, required)
    - `userId` (UUID, required)
- **Responses:**
  - `200 OK`: Updated `DeviceDTO` reflecting new `userId`.
  - `404 Not Found`: Device or User (Person) not found.

---

### 2.4 Internal Person Shadow Synchronization Endpoints

These endpoints operate on internal Docker network ports (`device-service:8080`) to guarantee data duplication and strong consistency between `user_db` and `device_db`.

- **`POST /person`**: Replicates a newly registered user ID, name, and username into the `persons` table in `device_db`.
- **`PUT /person/{id}`**: Updates shadow person name and username in `device_db`.
- **`DELETE /person/{id}`**: Disassociates all devices owned by the user (`SET user_id = NULL`) and deletes the shadow person record.
