# Energy Management System — Distributed Systems Assignment 1

**Institution:** Technical University of Cluj-Napoca (UTCN)  
**Faculty:** Faculty of Automation and Computer Science, Computer Science Department  
**Course:** Distributed Systems (Request-Reply Communication Paradigm)  
**Academic Year:** 2026–2027  
**Technology Stack:** Java 25 (LTS), Spring Boot 4.1.1, React 18, Vite, Nginx 1.27 (Alpine), MySQL 8.4 (LTS), Docker & Docker Compose v2  
**Key Deliverables:**
- [UML Deployment Diagram](DEPLOYMENT.md) ([SVG](deployment-diagram.svg), [PlantUML](deployment-diagram.puml))
- [Swagger / OpenAPI Documentation](SWAGGER.md) ([Interactive UI](swagger-ui.html) / `https://localhost/docs`)
- Database Dumps: [`db-init/init-auth.sql`](db-init/init-auth.sql), [`db-init/init-user.sql`](db-init/init-user.sql), [`db-init/init-device.sql`](db-init/init-device.sql)

---

## Table of Contents
1. [System Architecture & Components](#1-system-architecture--components)
2. [Prerequisites & Build Considerations](#2-prerequisites--build-considerations)
3. [Execution Guide (Quick Start)](#3-execution-guide-quick-start)
4. [Role-Based Access & Default Credentials](#4-role-based-access--default-credentials)
5. [Security & Gateway Token Validation Flow](#5-security--gateway-token-validation-flow)
6. [Data Consistency & User-Device Database Synchronization](#6-data-consistency--user-device-database-synchronization)
7. [API Specification (Swagger / OpenAPI)](#7-api-specification-swagger--openapi)
8. [UML Deployment Architecture](#8-uml-deployment-architecture)
9. [Answers to Theoretical Evaluation Questions (Section 3.1)](#9-answers-to-theoretical-evaluation-questions-section-31)
10. [GitLab Submission Guidelines](#10-gitlab-submission-guidelines)

---

## 1. System Architecture & Components

The application is engineered as a set of loosely coupled, containerized microservices following the **Database-per-Service** design pattern. All external incoming client traffic is routed through a centralized **Nginx Reverse Proxy & API Gateway** enforcing HTTPS and centralized token validation.

```
                              ┌──────────────────────────────────────────────────┐
                              │            Web Browser / Client App              │
                              └────────────────────────┬─────────────────────────┘
                                                       │ HTTPS (Port 443) / HTTP (Port 80)
                                                       ▼
                              ┌──────────────────────────────────────────────────┐
                              │            Nginx Reverse Proxy & Gateway         │
                              │           (TLS Termination, auth_request)        │
                              └───────┬──────────────┬──────────────┬────────────┘
                                      │              │              │
                   ┌──────────────────┼──────────────┼──────────────┼──────────────────┐
                   │                  │              │              │                  │
                   ▼                  ▼              ▼              ▼                  ▼
          ┌─────────────────┐ ┌──────────────┐ ┌───────────┐ ┌────────────┐ ┌───────────────────┐
          │    frontend     │ │ auth-service │ │user-serv. │ │device-serv.│ │ spring-demo-java25│
          │(React 18 / Vite)│ │   (:8080)    │ │  (:8080)  │ │  (:8080)   │ │     (:8080)       │
          └─────────────────┘ └───────┬──────┘ └─────┬─────┘ └──────┬─────┘ └─────────┬─────────┘
                                      │              │              │                 │
                                      ▼              ▼              ▼                 ▼
                              ┌──────────────┐ ┌───────────┐ ┌────────────┐ ┌───────────────────┐
                              │  mysql-auth  │ │mysql-user │ │mysql-device│ │     mysql-db      │
                              │  (auth_db)   │ │ (user_db) │ │(device_db) │ │   (energy_db)     │
                              └──────────────┘ └───────────┘ └────────────┘ └───────────────────┘
```

### Component Breakdown
1. **Frontend Application (`frontend`)**:
   - Modern React 18 Single-Page Application (SPA) bundled with Vite.
   - Credentials passed via HTTP `POST` body (never in URLs).
   - Session storage vault preserving JWT Bearer tokens and claims.
   - **Admin View:** Full CRUD management over user profiles, device entities, and user-to-device mapping.
   - **Client View:** Read-only dashboard showing only the devices assigned to the logged-in client account.
2. **Reverse Proxy & API Gateway (`nginx-proxy`)**:
   - Operates on ports `80` (HTTP redirect) and `443` (TLS 1.2/1.3 encrypted HTTPS).
   - Serves interactive Swagger API Documentation at `/docs`.
   - Protects downstream microservices using Nginx `auth_request /auth/validate`.
   - Injects user claims (`X-User-Id`, `X-Username`, `X-Role`) into upstream headers.
3. **Authentication Microservice (`auth-service`)**:
   - Handles login (`POST /auth/login`) and credential registration.
   - Secure password storage using `BCryptPasswordEncoder` (work factor 10).
   - Cryptographic token generation via `JJWT 0.12.6` using HMAC-SHA256 (`Keys.hmacShaKeyFor`).
   - Token validation endpoint (`GET /auth/validate`) consumed by Nginx.
4. **User Management Microservice (`user-service`)**:
   - Full CRUD operations for user entities stored in `user_db`.
   - Role-based authorization interceptor (`ADMIN` full access; `CLIENT` profile view only).
   - Synchronous, strong consistency coordinator that replicates user identity to `auth-service` and shadow person table in `device-service`.
5. **Device Management Microservice (`device-service`)**:
   - Full CRUD operations for smart energy metering devices in `device_db`.
   - Maintains a local `persons` shadow table mirroring user UUIDs, names, and usernames.
   - Enforces a true database relational foreign key (`devices.user_id` &rarr; `persons.id`).
   - Supports device assignment and bulk unassignment on user deletion.

---

## 2. Prerequisites & Build Considerations

### System Requirements
- **Docker Engine:** Version 24.0+ / Docker Desktop (Windows / macOS / Linux)
- **Docker Compose:** Version 2.20+ (Compose v2)
- **Ports Required on Host:**
  - `80` (HTTP Ingress - auto redirects to 443)
  - `443` (HTTPS TLS Ingress)
  - `3307`, `3308`, `3309`, `3310` (Direct database administration access)

### Multi-Stage Container Packaging
All microservices use optimized multi-stage Docker builds:
1. **Java Microservices (`auth-service`, `user-service`, `device-service`):**
   - Stage 1 (Builder): `maven:3.9-eclipse-temurin-25` resolves dependencies (`dependency:go-offline`) and compiles the Spring Boot JAR with skipped tests.
   - Layer Extraction: `JarLauncher` extracts application layers (`dependencies`, `spring-boot-loader`, `application`).
   - Stage 2 (Runtime): Minimal `eclipse-temurin:25-jre` executed under an unprivileged `spring` system user for hardened security.
2. **Frontend (`frontend`):**
   - Stage 1 (Builder): `node:20-alpine` runs `npm install` and `npm run build` creating the production bundle in `dist/`.
   - Stage 2 (Runtime): Lightweight `nginx:alpine` serving the static assets with client-side SPA routing support (`try_files $uri /index.html`).
3. **Gateway (`nginx`):**
   - Generates an X.509 self-signed TLS certificate (`/etc/nginx/ssl/server.crt`, `server.key`) during image build using `openssl`.

---

## 3. Execution Guide (Quick Start)

### Step 1: Clone Repository & Verify Configuration
```bash
git clone <your-repository-url>
cd Project1
```
Check that the `.env` file exists in the project root (default configuration provided).

### Step 2: Build and Launch All Containers
```bash
docker compose up --build -d
```
Docker Compose will download images, build the microservices in parallel, and start all containers with healthy dependencies.

### Step 3: Access the System
- **Web Application (React Frontend):**  
  👉 **`https://localhost`**  
  *(Note: Because the gateway uses a self-signed TLS certificate for local evaluation, click **Advanced &rarr; Proceed to localhost** in your browser).*
- **Interactive Swagger Documentation:**  
  👉 **`https://localhost/docs`** (or open [`swagger-ui.html`](swagger-ui.html) locally)
- **Gateway Health Check:**  
  👉 **`https://localhost/health`** (Returns `200 OK - 'nginx ok'`)

### Step 4: Stopping the Containers
To stop the environment and preserve persistent database storage:
```bash
docker compose down
```
To stop the environment and remove persistent database volumes:
```bash
docker compose down -v
```

---

## 4. Role-Based Access & Default Credentials

The databases are automatically pre-populated upon initial startup via the scripts in [`db-init/`](db-init/):

| Username | Plaintext Password | Role | Assigned UUID | Accessible Views & Permissions |
|---|---|---|---|---|
| **`admin`** | `admin123` | **`ADMIN`** | `11111111-1111-1111-1111-111111111111` | • Full CRUD on Users<br>• Full CRUD on Devices<br>• Assign/Unassign Devices to Users<br>• Swagger Documentation link |
| **`client1`**| `client123` | **`CLIENT`** | `22222222-2222-2222-2222-222222222222` | • View assigned devices only (`Smart Meter Living Room`, `Smart Meter Kitchen`)<br>• Access restricted from other users' profiles and devices |

---

## 5. Security & Gateway Token Validation Flow

To comply with the assignment requirements, security validation is handled at the **API Gateway (Nginx)**, not duplicated manually in each microservice:

```
[Browser] --- (1) POST /auth/login (username, password) ---> [Nginx Gateway]
                                                                   │
                                                                   ▼ (2) Proxy Pass
                                                          [auth-service]
                                                                   │
                                  <--- (3) 200 OK + JWT Token ─────┘
                                  
[Browser] --- (4) GET /user or /device + [Bearer Token] ---> [Nginx Gateway]
                                                                   │
                                                                   ▼ (5) auth_request /auth/validate
                                                          [auth-service]
                                                                   │
                                  <─── (6) 200 OK + Claims in Headers
                                       (X-User-Id, X-Username, X-Role)
                                                                   │
[Nginx Gateway] --- (7) Forward Request + Injected Claims ---> [user-service / device-service]
                                                                   │
                                                                   ▼ (8) RoleInterceptor
                                                              Check Role & User ID
```

### Key Security Properties:
1. **Zero Credential Exposure in URLs:** Credentials are submitted strictly via HTTP `POST` body with JSON payloads (`LoginRequestDTO`).
2. **Centralized Verification:** Microservices do not verify JWT secrets directly. The reverse proxy uses the `auth_request` directive to validate the Bearer token with `auth-service`.
3. **Internal Network Isolation:** Downstream microservices communicate on the internal bridge network (`demo_net`). Sensitive endpoints like `/person` and internal sync routes are not exposed publicly.
4. **Defense in Depth (`RoleInterceptor`):** Even if an unauthorized request reaches a microservice, each Spring Boot service has an interceptor checking the verified `X-Role` and `X-User-Id` headers. Missing headers trigger an immediate HTTP `403 Forbidden`.

---

## 6. Data Consistency & User-Device Database Synchronization

To maintain strict relational integrity in `device_db` while adhering to the Database-per-Service paradigm:

1. **The `persons` Shadow Table:**
   - The `device_db` database contains a `persons` table whose `id` column matches the exact UUID from `user_db.users`.
   - `devices.user_id` is defined as a formal relational foreign key constraint:
     ```sql
     CONSTRAINT fk_device_person FOREIGN KEY (user_id) REFERENCES persons(id) ON DELETE SET NULL
     ```
2. **Strong Consistency Lifecycle:**
   - **User Creation:** In `UserService.insert()`, after saving the user entity, synchronous REST calls register credentials in `auth-service` and create the shadow person in `device-service` (`POST /person`). If any call fails, an exception is thrown, rolling back the database transaction.
   - **User Update:** In `UserService.update()`, profile updates synchronously invoke `PUT /person/{id}` in `device-service`.
   - **User Deletion:** In `UserService.delete()`, `user-service` synchronously calls `device-service` (`DELETE /person/{id}`), which updates all associated devices (`SET user_id = NULL`) and removes the person record, then revokes authentication credentials, and finally removes the user record.

---

## 7. API Specification (Swagger / OpenAPI)

The complete API specification is available in multiple formats:
- **Markdown Specification:** [`SWAGGER.md`](SWAGGER.md) (Contains the exact path, HTTP method, short description, input params, and response matrix).
- **OpenAPI 3.0.3 Files:** [`openapi.yaml`](openapi.yaml) and [`openapi.json`](openapi.json).
- **Live Interactive UI:** Open `https://localhost/docs` in your browser or double-click [`swagger-ui.html`](swagger-ui.html).

### Summary Matrix

| Method | Endpoint | Description | Role Required |
|---|---|---|---|
| `POST` | `/auth/login` | Authenticate user and issue JWT | Public |
| `POST` | `/auth/register` | Register new user credentials | Public / Internal |
| `GET` | `/auth/validate` | Nginx internal token validator | Bearer Token |
| `DELETE`| `/auth/user/{username}`| Delete credential | Internal |
| `GET` | `/user` | List all users | `ADMIN` |
| `GET` | `/user/{id}` | Get user profile | `ADMIN` or profile owner (`CLIENT`) |
| `POST` | `/user` | Create user profile & sync | `ADMIN` |
| `PUT` | `/user/{id}` | Update user details & sync | `ADMIN` |
| `DELETE`| `/user/{id}` | Delete user & sync | `ADMIN` |
| `GET` | `/device` | List all smart meters | `ADMIN` |
| `GET` | `/device/{id}` | Get specific device | `ADMIN` |
| `GET` | `/device/user/{userId}`| Get devices for a user | `ADMIN` or device owner (`CLIENT`) |
| `POST` | `/device` | Create smart meter | `ADMIN` |
| `PUT` | `/device/{id}` | Update device config | `ADMIN` |
| `DELETE`| `/device/{id}` | Remove device from inventory | `ADMIN` |
| `PATCH` | `/device/{dId}/assign/{uId}` | Assign device to user | `ADMIN` |

---

## 8. UML Deployment Architecture

The system deployment conforms to the UML 2.5 standard and is fully documented in [`DEPLOYMENT.md`](DEPLOYMENT.md).

- **PlantUML Model:** [`deployment-diagram.puml`](deployment-diagram.puml)
- **High-Resolution Vector Graphic:** [`deployment-diagram.svg`](deployment-diagram.svg)

---

## 9. Answers to Theoretical Evaluation Questions (Section 3.1)

During grading and project defense, instructors assess theoretical understanding of distributed systems principles. Below are structured, comprehensive answers:

### 1. URI vs. URL
- **URI (Uniform Resource Identifier):** An identifier that names or locates a resource abstractly (e.g., `urn:isbn:0451450523`, `tel:+1-816-555-1212`, or `https://localhost/user/123`).
- **URL (Uniform Resource Locator):** A specialized subset of URI that specifies *where* a resource is located and *how* to access it by defining the explicit network protocol and access mechanism (e.g., `https://localhost:443/user/123`).
- *Summary:* All URLs are URIs, but not all URIs are URLs.

### 2. Web Clients vs. Web Servers
- **Web Client:** A software application (e.g., Google Chrome, React SPA, cURL, Postman) that initiates HTTP/HTTPS requests to consume services, parse responses, and render visual content.
- **Web Server:** A network daemon (e.g., Nginx, Apache HTTPd, embedded Tomcat in Spring Boot) that listens on a dedicated TCP socket, accepts incoming connections, routes requests to application handlers, and transmits HTTP responses back to the client.

### 3. HTTP Protocol & HTTP Methods
- **HTTP (Hypertext Transfer Protocol):** An application-layer, stateless, request-reply communication protocol operating over TCP/IP (or QUIC in HTTP/3).
- **Core HTTP Methods & Semantics:**
  - `GET`: Safe and idempotent; retrieves resource representation without modifying server state.
  - `POST`: Non-idempotent; submits entity data to create new subordinate resources or trigger state transitions.
  - `PUT`: Idempotent; replaces the target resource entirely with the request payload.
  - `PATCH`: Non-idempotent (or conditionally idempotent); applies partial updates to a resource.
  - `DELETE`: Idempotent; removes the designated resource from the system.
  - `OPTIONS`: Safe; queries communication options and supported CORS preflight headers.

### 4. Reverse Proxy vs. Forward Proxy
- **Forward Proxy:** Acts on behalf of *clients* to access external servers, commonly used in corporate networks for egress filtering, content caching, and anonymity.
- **Reverse Proxy (used in this project via Nginx):** Acts on behalf of *servers*. Clients interact with the reverse proxy as if it were the origin server. It handles:
  1. Centralized TLS/SSL termination.
  2. Ingress request routing and load balancing.
  3. Security policy enforcement (centralized JWT authentication via `auth_request`).
  4. Network shielding (preventing direct external exposure of backend microservices).

### 5. Query Strings vs. Hidden Variables
- **Query Strings:** Key-value pairs appended directly to the URL following a `?` character (e.g., `/user?role=ADMIN&page=1`). Visible in browser history, bookmarks, proxy logs, and referrer headers; unsuitable for sensitive information like passwords.
- **Hidden Variables:** Historical web technique using HTML `<input type="hidden">` inside form bodies. Data is transmitted in the HTTP request body (`POST`) rather than the URL string. Modern SPAs instead transmit structured JSON payloads inside the HTTP request body.

### 6. Cookies vs. Session vs. JWT
- **Cookies:** Small key-value pairs stored on the client browser, automatically attached by the browser in the `Cookie` header on subsequent requests to the issuing domain.
- **Session (Server-Side):** The server maintains state (e.g., in memory or Redis) associated with a unique session ID. The client holds only the session ID (usually stored in a cookie). Drawback in distributed architectures: requires sticky sessions or shared session caches.
- **JWT (JSON Web Token — used in this project):** A stateless, self-contained, digitally signed token. The client stores it in `sessionStorage` and attaches it via the `Authorization: Bearer <token>` header.
  - *Structure:* Composed of three Base64URL-encoded segments separated by dots: `Header.Payload.Signature`.
  - *Benefits:* Microservices can verify token validity without querying a centralized session database.

### 7. Object-Relational Mapping (ORM)
- **ORM:** A software engineering technique (implemented here via **Hibernate 7 / Spring Data JPA**) that maps object-oriented domain models in Java (`@Entity`) to relational database tables and foreign keys.
- **Advantages:** Eliminates boilerplate SQL, prevents SQL injection vulnerabilities through parameterized queries, manages transactions (`@Transactional`), and automates object lifecycle caching (first-level / second-level cache).

### 8. REST Services (Representational State Transfer)
- An architectural style formulated by Roy Fielding governed by 6 core constraints:
  1. **Client-Server Separation:** Distinct separation of concerns between user interface and data persistence.
  2. **Statelessness:** Each request from client to server must contain all information required to understand and execute the request.
  3. **Cacheability:** Responses must define whether they are cacheable to prevent redundant processing.
  4. **Layered System:** The client cannot ordinarily tell whether it is connected directly to the end server or an intermediary (e.g., our Nginx gateway).
  5. **Uniform Interface:** Standardized resource identification (URIs), representation manipulation, and self-descriptive messages.
  6. **Code on Demand (Optional):** Ability for servers to transfer executable code (e.g., JavaScript).

---

## 10. GitLab Submission Guidelines

Per the assignment specification (Section 2):
1. **Repository Naming Convention:**
   ```
   DS2026_Group_LastName_FirstName_Assignment_1
   ```
   *(Example: `DS2026_30441_Popescu_Ion_Assignment_1`)*
2. **Required Files to Commit:**
   - Full microservices source code (`auth-service/`, `user-service/`, `device-service/`, `frontend/`, `nginx/`).
   - Container configuration: [`docker-compose.yml`](docker-compose.yml), [`.env.example`](.env.example).
   - Database seed initialization dumps: [`db-init/`](db-init/).
   - Architectural & Deployment documentation: [`README.md`](README.md), [`DEPLOYMENT.md`](DEPLOYMENT.md), [`deployment-diagram.svg`](deployment-diagram.svg), [`deployment-diagram.puml`](deployment-diagram.puml).
   - API Specifications: [`SWAGGER.md`](SWAGGER.md), [`openapi.json`](openapi.json), [`openapi.yaml`](openapi.yaml), [`swagger-ui.html`](swagger-ui.html).
3. **Sharing with Evaluator:**
   - Navigate to **Project Settings &rarr; Members** in GitLab.
   - Invite and grant `Developer` or `Maintainer` role to: **`utcn_dsrl`**.
