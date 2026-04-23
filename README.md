# Smart Campus API

**Module:** 5COSC022W — Client-Server Architectures  
**Academic Year:** 2025/26  
**Technology:** JAX-RS (Jakarta RESTful Web Services) · Jersey 3.1 · Grizzly HTTP Server · Maven

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Features Implemented](#2-features-implemented)
3. [Tech Stack](#3-tech-stack)
4. [Architecture Overview](#4-architecture-overview)
5. [Data Model Summary](#5-data-model-summary)
6. [How to Build](#6-how-to-build)
7. [How to Run](#7-how-to-run)
8. [API Endpoints](#8-api-endpoints)
9. [Example Request / Response Payloads](#9-example-request--response-payloads)
10. [curl Commands](#10-curl-commands)
11. [Error Handling Summary](#11-error-handling-summary)
12. [Logging Overview](#12-logging-overview)
13. [Assumptions and Design Decisions](#13-assumptions-and-design-decisions)
14. [Coursework Question Answers](#14-coursework-question-answers)

---

## 1. Project Overview

The Smart Campus API is a fully in-memory RESTful web service built with **JAX-RS / Jakarta RESTful Web Services** for the University of Westminster module 5COSC022W. It manages physical campus infrastructure — specifically **Rooms**, **Sensors**, and **SensorReadings** — exposing a clean, versioned REST API at `/api/v1`.

The system is designed to demonstrate:

- Correct HTTP semantics and status codes
- Nested resource design and sub-resource locators
- Custom exception handling and consistent error responses
- Request/response logging via container filters
- In-memory, thread-safe data management (no database)

---

## 2. Features Implemented

| Feature | Status |
|---|---|
| Discovery endpoint (`GET /api/v1`) | ✅ |
| Full CRUD for Rooms | ✅ |
| Full CRUD for Sensors with type filtering | ✅ |
| Sub-resource locator pattern for Readings | ✅ |
| POST/GET Sensor Readings | ✅ |
| `RoomNotEmptyException` → 409 | ✅ |
| `LinkedResourceNotFoundException` → 422 | ✅ |
| `SensorUnavailableException` → 403 | ✅ |
| Global exception mapper → 500 (no stack traces) | ✅ |
| Request/Response logging filter | ✅ |
| API versioning via `@ApplicationPath("/api/v1")` | ✅ |
| Thread-safe in-memory storage | ✅ |
| Seed data for immediate testing | ✅ |
| Location header on 201 Created responses | ✅ |

---

## 3. Tech Stack

| Component | Technology |
|---|---|
| Language | Java 11+ |
| JAX-RS Implementation | Jersey 3.1.3 |
| HTTP Server | Grizzly2 (embedded, no external container needed) |
| JSON | Jackson (via `jersey-media-json-jackson`) |
| Build Tool | Maven 3.8+ |
| Storage | In-memory (`ConcurrentHashMap`, `CopyOnWriteArrayList`) |

---

## 4. Architecture Overview

```
com.westminster.smartcampus
├── Main.java                        — Server entry point
├── config/
│   └── AppConfig.java               — @ApplicationPath("/api/v1"), registers all components
├── model/
│   ├── Room.java                    — Room POJO
│   ├── Sensor.java                  — Sensor POJO
│   ├── SensorReading.java           — SensorReading POJO
│   └── SensorStatus.java            — Enum: ACTIVE, MAINTENANCE, OFFLINE
├── dto/
│   ├── CreateRoomRequest.java       — Request DTO for POST /rooms
│   ├── CreateSensorRequest.java     — Request DTO for POST /sensors
│   ├── CreateReadingRequest.java    — Request DTO for POST /sensors/{id}/readings
│   └── ErrorResponse.java           — Uniform error payload DTO
├── store/
│   └── InMemoryStore.java           — Singleton thread-safe in-memory data store
├── resource/
│   ├── DiscoveryResource.java       — GET /api/v1
│   ├── RoomResource.java            — /api/v1/rooms
│   ├── SensorResource.java          — /api/v1/sensors + sub-resource locator
│   └── SensorReadingResource.java   — /api/v1/sensors/{id}/readings (sub-resource)
├── exception/
│   ├── RoomNotEmptyException.java
│   ├── LinkedResourceNotFoundException.java
│   └── SensorUnavailableException.java
├── mapper/
│   ├── RoomNotEmptyExceptionMapper.java
│   ├── LinkedResourceNotFoundExceptionMapper.java
│   ├── SensorUnavailableExceptionMapper.java
│   └── GlobalExceptionMapper.java
└── filter/
    └── ApiLoggingFilter.java        — Logs all requests and responses
```

### Request Flow

```
HTTP Client
    │
    ▼
Grizzly HTTP Server (port 8080)
    │
    ▼
ApiLoggingFilter (request)
    │
    ▼
JAX-RS Resource Method
    │
    ├── Happy path  ──────────────────────► JSON Response
    │
    └── Exception thrown
            │
            ▼
        ExceptionMapper
            │
            ▼
        ApiLoggingFilter (response)
            │
            ▼
        JSON Error Response
```

---

## 5. Data Model Summary

### Room
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 60,
  "sensorIds": ["CO2-001", "TEMP-001"]
}
```

### Sensor
```json
{
  "id": "CO2-001",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 420.0,
  "roomId": "LIB-301"
}
```
Valid `status` values: `ACTIVE`, `MAINTENANCE`, `OFFLINE`

### SensorReading
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1710000000000,
  "value": 435.7
}
```
IDs are auto-generated UUIDs. Timestamps are Unix milliseconds (auto-set by the server).

---

## 6. How to Build

**Prerequisites:** Java 11+, Maven 3.8+

```bash
# Clone or unzip the project, then:
cd smart-campus-api

# Compile and package into a single executable fat JAR
mvn clean package
```

The fat JAR is produced at `target/smart-campus-api-1.0.0.jar`.

---

## 7. How to Run

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts on **port 8080**. The API base URL is:

```
http://localhost:8080/api/v1
```

Press **ENTER** in the terminal to stop the server gracefully.

Seed data is loaded automatically at startup — you can query rooms and sensors immediately without any setup.

---

## 8. API Endpoints

### Discovery

| Method | Path | Description | Status |
|---|---|---|---|
| GET | `/api/v1` | API metadata and resource links | 200 |

### Rooms

| Method | Path | Description | Success | Error |
|---|---|---|---|---|
| GET | `/api/v1/rooms` | List all rooms | 200 | — |
| POST | `/api/v1/rooms` | Create a room | 201 | 400, 409 |
| GET | `/api/v1/rooms/{roomId}` | Get room by ID | 200 | 404 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room (must be empty) | 204 | 404, 409 |

### Sensors

| Method | Path | Description | Success | Error |
|---|---|---|---|---|
| GET | `/api/v1/sensors` | List all sensors | 200 | — |
| GET | `/api/v1/sensors?type=CO2` | Filter sensors by type | 200 | — |
| POST | `/api/v1/sensors` | Create a sensor | 201 | 400, 409, 422 |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor by ID | 200 | 404 |

### Sensor Readings (sub-resource)

| Method | Path | Description | Success | Error |
|---|---|---|---|---|
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor | 200 | 404 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a reading to a sensor | 201 | 400, 403, 404 |

---

## 9. Example Request / Response Payloads

### POST /api/v1/rooms — Create a Room

**Request body:**
```json
{
  "id": "LAB-202",
  "name": "Physics Lab",
  "capacity": 25
}
```

**Response 201 Created:**
```json
{
  "id": "LAB-202",
  "name": "Physics Lab",
  "capacity": 25,
  "sensorIds": []
}
```

---

### POST /api/v1/sensors — Create a Sensor

**Request body:**
```json
{
  "id": "TEMP-002",
  "type": "TEMPERATURE",
  "status": "ACTIVE",
  "currentValue": 21.0,
  "roomId": "LAB-202"
}
```

**Response 201 Created:**
```json
{
  "id": "TEMP-002",
  "type": "TEMPERATURE",
  "status": "ACTIVE",
  "currentValue": 21.0,
  "roomId": "LAB-202"
}
```

---

### POST /api/v1/sensors/TEMP-002/readings — Add a Reading

**Request body:**
```json
{ "value": 23.4 }
```

**Response 201 Created:**
```json
{
  "id": "a3f5c2d1-...",
  "timestamp": 1710000000000,
  "value": 23.4
}
```

---

### Error — POST with invalid roomId (422)

**Request body:**
```json
{
  "id": "CO2-999",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 400.0,
  "roomId": "ROOM-DOES-NOT-EXIST"
}
```

**Response 422:**
```json
{
  "error": "Linked Resource Not Found",
  "message": "The referenced room with id 'ROOM-DOES-NOT-EXIST' does not exist. The request is well-formed but references an unknown resource.",
  "status": 422,
  "timestamp": 1710000000000
}
```

---

### Error — DELETE room with sensors (409)

**Response 409:**
```json
{
  "error": "Room Not Empty",
  "message": "Room LIB-301 cannot be deleted because it still has 2 assigned sensor(s). Reassign or remove all sensors first.",
  "status": 409,
  "timestamp": 1710000000000
}
```

---

### Error — POST reading to MAINTENANCE sensor (403)

**Response 403:**
```json
{
  "error": "Sensor Unavailable",
  "message": "Sensor 'CO2-002' is currently under MAINTENANCE and cannot accept new readings.",
  "status": 403,
  "timestamp": 1710000000000
}
```

---

## 10. curl Commands

### 1. GET API Discovery
```bash
curl -s http://localhost:8080/api/v1 | python3 -m json.tool
```

### 2. GET All Rooms
```bash
curl -s http://localhost:8080/api/v1/rooms | python3 -m json.tool
```

### 3. POST — Create a Room
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LAB-202","name":"Physics Lab","capacity":25}' | python3 -m json.tool
```

### 4. GET Room by ID
```bash
curl -s http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 5. POST — Create a Sensor (valid roomId)
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"TEMPERATURE","status":"ACTIVE","currentValue":21.0,"roomId":"LAB-202"}' \
  | python3 -m json.tool
```

### 6. GET All Sensors (no filter)
```bash
curl -s http://localhost:8080/api/v1/sensors | python3 -m json.tool
```

### 7. GET Sensors filtered by type
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2" | python3 -m json.tool
```

### 8. POST — Add a Reading to an ACTIVE sensor
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":435.7}' | python3 -m json.tool
```

### 9. GET All Readings for a Sensor
```bash
curl -s http://localhost:8080/api/v1/sensors/CO2-001/readings | python3 -m json.tool
```

### 10. GET Sensor — verify currentValue updated after reading
```bash
curl -s http://localhost:8080/api/v1/sensors/CO2-001 | python3 -m json.tool
```

### 11. DELETE — Room with no sensors (success, 204)
```bash
# First create a room with no sensors
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"EMPTY-ROOM","name":"Empty Test Room","capacity":10}'

# Now delete it
curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/api/v1/rooms/EMPTY-ROOM
```

### 12. DELETE — Room with sensors → 409 Conflict
```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

### 13. POST Sensor with non-existent roomId → 422
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-999","type":"CO2","status":"ACTIVE","currentValue":400.0,"roomId":"GHOST-ROOM"}' \
  | python3 -m json.tool
```

### 14. POST Reading to MAINTENANCE sensor → 403
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/CO2-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value":399.0}' | python3 -m json.tool
```

### 15. GET non-existent room → 404
```bash
curl -s http://localhost:8080/api/v1/rooms/DOES-NOT-EXIST | python3 -m json.tool
```

---

## 11. Error Handling Summary

| Scenario | Exception Class | HTTP Status | Mapper |
|---|---|---|---|
| Delete room with sensors | `RoomNotEmptyException` | 409 Conflict | `RoomNotEmptyExceptionMapper` |
| Create sensor, roomId not found | `LinkedResourceNotFoundException` | 422 Unprocessable Entity | `LinkedResourceNotFoundExceptionMapper` |
| Post reading to MAINTENANCE sensor | `SensorUnavailableException` | 403 Forbidden | `SensorUnavailableExceptionMapper` |
| Missing resource (room/sensor) | Inline check | 404 Not Found | — |
| Invalid request payload | Inline check | 400 Bad Request | — |
| Duplicate ID | Inline check | 409 Conflict | — |
| Any unexpected runtime error | Any `Throwable` | 500 Internal Server Error | `GlobalExceptionMapper` |

All error responses follow this uniform JSON structure:
```json
{
  "error": "Short Error Title",
  "message": "Human-readable explanation.",
  "status": 409,
  "timestamp": 1710000000000
}
```

No Java stack traces, class names, or internal details are ever exposed in API responses.

---

## 12. Logging Overview

The `ApiLoggingFilter` class implements both `ContainerRequestFilter` and `ContainerResponseFilter`. It logs every HTTP request and its corresponding response status using `java.util.logging.Logger`.

Example log output:
```
INFO: [REQUEST]  GET http://localhost:8080/api/v1/rooms
INFO: [RESPONSE] GET http://localhost:8080/api/v1/rooms -> HTTP 200
INFO: [REQUEST]  DELETE http://localhost:8080/api/v1/rooms/LIB-301
INFO: [RESPONSE] DELETE http://localhost:8080/api/v1/rooms/LIB-301 -> HTTP 409
```

This approach keeps logging as a cross-cutting concern separate from business logic.

---

## 13. Assumptions and Design Decisions

| Decision | Rationale |
|---|---|
| Sensor `id` is client-supplied | Allows predictable IDs like `CO2-001` for demo clarity. UUID auto-generation was chosen only for reading IDs since they have no meaningful name. |
| `timestamp` in readings is server-set | Avoids clock-skew issues. The server stamps the reading with `System.currentTimeMillis()` at the moment of receipt. Clients only submit `value`. |
| `status` defaults to `ACTIVE` if omitted | Reduces required fields for simple sensor creation. |
| `DELETE /rooms/{id}` on missing room → 404 | Chosen over silent 204 to give clear feedback. This does not violate REST idempotency semantics — see question answers below. |
| `GET /sensors?type=CO2` is case-insensitive | More user-friendly; documented in endpoint table. |
| `SensorReadingResource` has no `@Path` class annotation | It is a pure sub-resource class, accessed only via the locator in `SensorResource`, as per the JAX-RS sub-resource locator pattern. |
| `InMemoryStore` is a singleton | JAX-RS resource classes are by default instantiated per request. A singleton store ensures shared state across all requests. |
| `ResourceConfig` used instead of plain `Application` | Jersey's `ResourceConfig` extends `Application` and provides a cleaner registration API. The `@ApplicationPath("/api/v1")` annotation is standard JAX-RS. |

---

## 14. Coursework Question Answers

### Part 1 — JAX-RS Resource Lifecycle and HATEOAS

**Q: Explain the default lifecycle of a JAX-RS resource class. Is a new instance created per request or is it a singleton?**

By default, JAX-RS creates a **new instance of a resource class for every incoming HTTP request**. This is known as the *per-request* lifecycle. Each request gets a fresh, independent instance, which avoids state contamination between concurrent requests. However, this means that instance fields are not shared across requests — they are re-initialised for every call.

**Q: How does the default per-request lifecycle affect managing shared in-memory data?**

Because each resource class instance is discarded after a request, you cannot store shared data in instance fields of the resource class. Any data that must persist across requests — such as the rooms, sensors, and readings collections in this project — must be held in an **application-scoped object** that lives outside the resource lifecycle. In this implementation, `InMemoryStore.getInstance()` provides a singleton that holds all shared state. The resource classes obtain a reference to it on each instantiation, ensuring all instances read and write the same underlying collections.

**Q: Why is HATEOAS (Hypermedia as the Engine of Application State) valuable?**

HATEOAS extends REST by embedding hyperlinks in API responses, pointing clients to the next valid actions. For example, a response for a newly created room might include a link to its sensors. This makes the API self-documenting at runtime: clients can discover available operations dynamically rather than relying on static external documentation. It reduces client-side coupling to URL structures, meaning that if the server changes a URL, clients that follow links rather than hardcoding paths will adapt automatically.

---

### Part 2 — Room List Design and DELETE Idempotency

**Q: Should a room list response return only IDs or full room objects? Discuss bandwidth and client-side processing trade-offs.**

Returning only IDs is bandwidth-efficient and is the right choice for very large collections, but it forces clients to make N additional requests to retrieve room details — the "N+1 request" problem. Returning full room objects increases response size but allows the client to render a complete list in one round trip. For this campus system, where room counts are modest, returning full objects is the better trade-off. If scale were a concern, pagination (returning partial lists with `next`/`prev` links) would address bandwidth without sacrificing usability.

**Q: Is DELETE idempotent in this implementation? Explain what happens on repeated DELETE calls.**

Yes, DELETE is **idempotent** by REST definition, and this implementation honours that principle. The *effect* of the DELETE — that the resource no longer exists — is the same whether it is called once or many times. The server state after the first successful DELETE (resource gone) equals the server state after the second call (resource still gone). The HTTP status code differs: the first call returns `204 No Content`; subsequent calls return `404 Not Found` because the resource was already removed. Returning 404 on repeated DELETE is debatable — some APIs return 204 every time — but returning 404 is equally valid and arguably more honest about the server's state.

---

### Part 3 — Content Negotiation and Query Parameter Filtering

**Q: What happens if a client sends `Content-Type: text/plain` or `application/xml` to a JSON-only POST endpoint?**

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST methods tells JAX-RS that only requests with `Content-Type: application/json` are accepted. If a client sends `text/plain` or `application/xml`, JAX-RS (Jersey) will respond with **415 Unsupported Media Type** before the resource method is ever invoked. This is handled automatically by the framework — no custom code is needed.

**Q: Why is query parameter filtering (`GET /sensors?type=CO2`) better than path-based filtering for collection searches?**

Path-based filtering (`GET /sensors/type/CO2`) would suggest that `type/CO2` is a distinct named resource, which it is not — it is a filter applied to the sensors collection. Query parameters are semantically correct for filtering, sorting, and pagination because they modify the retrieval of a resource rather than identify one. They are also optional by nature, which means `GET /sensors` and `GET /sensors?type=CO2` share the same resource path (`/sensors`) and the filter is additive. This is consistent with REST conventions and makes the URL structure cleaner and easier to extend.

---

### Part 4 — Sub-Resource Locator Pattern

**Q: What are the benefits of the sub-resource locator pattern?**

A sub-resource locator is a method that returns an object (another resource class instance) rather than a response. JAX-RS delegates further path matching to that returned object. The benefits are:

1. **Separation of concerns** — the reading logic is encapsulated in `SensorReadingResource`, not mixed into `SensorResource`.
2. **Reusability** — `SensorReadingResource` could theoretically be reused for different parent resource types.
3. **Clarity** — the parent resource (`SensorResource`) acts as a router, making the intent clear: "if the path goes deeper, hand off to this specialist class."
4. **Testability** — sub-resource classes can be unit-tested independently with a constructed `sensorId`.

**Q: Why is it better than putting all nested paths in one giant controller class?**

A single class handling `/sensors`, `/sensors/{id}`, and `/sensors/{id}/readings` would violate the Single Responsibility Principle. It would grow large, become harder to read, and mix the concerns of sensor management with reading management. The sub-resource locator pattern enforces clean modular boundaries: each class has a well-defined scope and set of responsibilities.

---

### Part 5 — 422 vs 404, Stack Trace Security, and Filter Logging

**Q: Why can 422 be more semantically correct than 404 when a linked resource is missing from a valid JSON payload?**

HTTP 404 means the *requested resource* was not found — i.e., the URL the client is calling does not exist. HTTP 422 means the request was syntactically valid (correct JSON, correct Content-Type) but semantically it cannot be processed — for example, the `roomId` field points to a room that does not exist. The distinction is important: the client is not asking for the wrong URL; they are providing business-layer data that violates a referential integrity rule. Returning 422 communicates this distinction precisely, whereas 404 would mislead the client into thinking they called the wrong endpoint.

**Q: Why is exposing Java stack traces a cybersecurity risk?**

Stack traces reveal internal implementation details: package names, class names, library versions, file paths, and the full call stack. An attacker can use this to:
- Identify specific versions of libraries with known CVEs (Common Vulnerabilities and Exposures)
- Understand the internal architecture to craft targeted injection attacks
- Discover unexpected code paths that might be exploitable

This project's `GlobalExceptionMapper` ensures that all unexpected exceptions are logged server-side (with full detail for developers) while returning only a generic, safe message to the client.

**Q: Why are filters better than `Logger.info()` in every method for cross-cutting concerns like logging?**

Manually adding logging calls to every resource method violates the DRY (Don't Repeat Yourself) principle and scatters infrastructure concerns throughout business logic. If the logging format changes, every method must be updated. A `ContainerRequestFilter` / `ContainerResponseFilter` implementation runs automatically for every request/response without touching the resource classes at all. This is the standard aspect-oriented approach to cross-cutting concerns: the filter is registered once and applied everywhere, keeping resource methods focused purely on business logic.

---

*End of README*
