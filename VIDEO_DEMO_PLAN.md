# Video Demonstration Plan
**Module:** 5COSC022W — Client-Server Architectures  
**Submission:** Blackboard video, max 10 minutes  
**Tool:** Postman  
**Requirement:** Be present on screen or speak clearly throughout

---

## Pre-Demo Checklist (do before recording)

- [ ] Run `mvn clean package` — confirm BUILD SUCCESS
- [ ] Start server: `java -jar target/smart-campus-api-1.0.0.jar`
- [ ] Confirm logs show: `Smart Campus API started successfully`
- [ ] Open Postman and clear any previous collections
- [ ] Create a new Postman collection called **"Smart Campus API Demo"**
- [ ] Set font size large enough to be clearly readable on screen
- [ ] Open terminal side-by-side with Postman so logs are visible

---

## Time Budget

| Step | Time |
|---|---|
| Introduction | ~30s |
| Server startup + project overview | ~45s |
| Discovery endpoint | ~30s |
| Room operations | ~1m 30s |
| Sensor operations | ~1m 30s |
| Readings + currentValue update | ~1m |
| Error scenarios (409, 422, 403, 404) | ~2m |
| Logging + exception mappers mention | ~45s |
| Conclusion | ~30s |
| **Total** | **~9 minutes** |

---

## Demonstration Sequence

---

### Step 0 — Introduction (0:00 – 0:30)

**Say:**
> "Hi, my name is [your name], student number [your number]. This is my submission for 5COSC022W Client-Server Architectures. I've built a Smart Campus REST API using JAX-RS and Jersey, with an embedded Grizzly HTTP server and fully in-memory storage — no database. The API is versioned at /api/v1 and manages rooms, sensors, and sensor readings."

**Show:** Your IDE with the project structure open — briefly scroll through the packages: config, model, dto, store, resource, exception, mapper, filter.

---

### Step 1 — Start the Server (0:30 – 1:15)

**Do:** In terminal, run:
```
java -jar target/smart-campus-api-1.0.0.jar
```

**Show:** Terminal output with the startup log lines, especially:
```
Smart Campus API started successfully.
Base URL : http://localhost:8080/api/v1
```

**Say:**
> "The server starts with a single command. All seed data — three rooms and four sensors — is loaded automatically at startup from the InMemoryStore singleton. No database setup is needed."

---

### Step 2 — Discovery Endpoint (1:15 – 1:45)

**Postman Request:**
- Method: `GET`
- URL: `http://localhost:8080/api/v1`

**Expected Response (200 OK):**
```json
{
  "name": "Smart Campus API",
  "version": "v1",
  "contact": "backend-admin@westminster.example",
  "timestamp": 1710000000000,
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

**Say:**
> "The discovery endpoint returns API metadata and links to the primary resource collections. This is a basic form of HATEOAS — clients can discover available resources without reading external documentation."

**Show:** The terminal log line `[REQUEST] GET http://localhost:8080/api/v1` and `[RESPONSE] ... -> HTTP 200`. Point out the logging filter working.

---

### Step 3 — List All Rooms (1:45 – 2:15)

**Postman Request:**
- Method: `GET`
- URL: `http://localhost:8080/api/v1/rooms`

**Expected Response (200 OK):**
```json
[
  { "id": "LIB-301", "name": "Library Quiet Study", "capacity": 60, "sensorIds": ["CO2-001","TEMP-001"] },
  { "id": "LAB-101", "name": "Computer Lab 1", "capacity": 30, "sensorIds": ["CO2-002"] },
  { "id": "HALL-A",  "name": "Main Hall A",     "capacity": 200, "sensorIds": ["HUM-001"] }
]
```

**Say:**
> "GET /rooms returns all rooms with their full details, including the list of sensor IDs currently assigned to each room. The seed data is already loaded."

---

### Step 4 — Create a New Room (2:15 – 2:45)

**Postman Request:**
- Method: `POST`
- URL: `http://localhost:8080/api/v1/rooms`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "id": "LAB-202",
  "name": "Physics Lab",
  "capacity": 25
}
```

**Expected Response (201 Created):**
```json
{
  "id": "LAB-202",
  "name": "Physics Lab",
  "capacity": 25,
  "sensorIds": []
}
```

**Show:** The `Location` header in the response: `http://localhost:8080/api/v1/rooms/LAB-202`

**Say:**
> "POST /rooms creates a new room and returns 201 Created with the full room object. Notice the Location header pointing to the new resource — this follows REST best practice for creation responses. The sensorIds list starts empty."

---

### Step 5 — Get Room by ID (2:45 – 3:00)

**Postman Request:**
- Method: `GET`
- URL: `http://localhost:8080/api/v1/rooms/LAB-202`

**Say:**
> "We can fetch a specific room by ID. 200 OK with the room object."

---

### Step 6 — Create a Sensor (3:00 – 3:30)

**Postman Request:**
- Method: `POST`
- URL: `http://localhost:8080/api/v1/sensors`
- Headers: `Content-Type: application/json`
- Body:
```json
{
  "id": "TEMP-002",
  "type": "TEMPERATURE",
  "status": "ACTIVE",
  "currentValue": 21.0,
  "roomId": "LAB-202"
}
```

**Expected Response (201 Created):**
```json
{
  "id": "TEMP-002",
  "type": "TEMPERATURE",
  "status": "ACTIVE",
  "currentValue": 21.0,
  "roomId": "LAB-202"
}
```

**Say:**
> "Creating a sensor requires a valid roomId. On success, the sensor is stored and the parent room's sensorIds list is automatically updated. Let me verify that."

**Follow-up request:** `GET http://localhost:8080/api/v1/rooms/LAB-202` — show `sensorIds` now includes `"TEMP-002"`.

---

### Step 7 — Filter Sensors by Type (3:30 – 4:00)

**Postman Request:**
- Method: `GET`
- URL: `http://localhost:8080/api/v1/sensors?type=CO2`

**Expected Response (200 OK):**
Array containing only sensors where `type` equals `CO2`.

**Say:**
> "The sensors collection supports optional filtering via a query parameter. GET /sensors with no parameter returns everything. Adding ?type=CO2 returns only CO2 sensors. Query parameters are the correct REST mechanism for filtering — not path segments."

---

### Step 8 — Add a Reading to an ACTIVE Sensor (4:00 – 4:45)

**Postman Request:**
- Method: `POST`
- URL: `http://localhost:8080/api/v1/sensors/CO2-001/readings`
- Headers: `Content-Type: application/json`
- Body:
```json
{ "value": 445.2 }
```

**Expected Response (201 Created):**
```json
{
  "id": "a3f5c2d1-...",
  "timestamp": 1710000000000,
  "value": 445.2
}
```

**Say:**
> "This uses the sub-resource locator pattern. SensorResource delegates the /readings path to a dedicated SensorReadingResource class. The reading ID is auto-generated as a UUID and the timestamp is set server-side. The client only needs to provide the value."

**Follow-up:** `GET http://localhost:8080/api/v1/sensors/CO2-001` — show `currentValue` is now `445.2`.

**Say:**
> "After adding a reading, the parent sensor's currentValue is automatically updated to the latest reading value."

**Follow-up:** `GET http://localhost:8080/api/v1/sensors/CO2-001/readings` — show the full list of historical readings.

---

### Step 9 — Error: DELETE Room With Sensors → 409 (4:45 – 5:30)

**Postman Request:**
- Method: `DELETE`
- URL: `http://localhost:8080/api/v1/rooms/LIB-301`

**Expected Response (409 Conflict):**
```json
{
  "error": "Room Not Empty",
  "message": "Room LIB-301 cannot be deleted because it still has 2 assigned sensor(s). Reassign or remove all sensors first.",
  "status": 409,
  "timestamp": 1710000000000
}
```

**Say:**
> "Attempting to delete a room that still has sensors assigned throws a RoomNotEmptyException. The RoomNotEmptyExceptionMapper converts it to a 409 Conflict with a clear JSON error body explaining exactly why the deletion was refused."

**Then:** `DELETE http://localhost:8080/api/v1/rooms/LAB-202` (which has no sensors — wait, we added TEMP-002 to it). If TEMP-002 was added, the room now has sensors. Instead use a freshly created empty room.

**Alternative:** Create `EMPTY-001` room, then immediately delete it to show 204 No Content.

---

### Step 10 — Error: Create Sensor with Invalid roomId → 422 (5:30 – 6:15)

**Postman Request:**
- Method: `POST`
- URL: `http://localhost:8080/api/v1/sensors`
- Body:
```json
{
  "id": "CO2-999",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 400.0,
  "roomId": "GHOST-ROOM"
}
```

**Expected Response (422 Unprocessable Entity):**
```json
{
  "error": "Linked Resource Not Found",
  "message": "The referenced room with id 'GHOST-ROOM' does not exist. The request is well-formed but references an unknown resource.",
  "status": 422,
  "timestamp": 1710000000000
}
```

**Say:**
> "The JSON here is perfectly valid — it's well-formed and has all required fields. The problem is semantic: the roomId 'GHOST-ROOM' doesn't exist. This is why we use 422 Unprocessable Entity rather than 404 — 404 would wrongly imply the client called the wrong URL. 422 says: your request was understood but the business logic cannot process it."

---

### Step 11 — Error: POST Reading to MAINTENANCE Sensor → 403 (6:15 – 7:00)

**Postman Request:**
- Method: `POST`
- URL: `http://localhost:8080/api/v1/sensors/CO2-002/readings`
- Body:
```json
{ "value": 399.0 }
```

**Expected Response (403 Forbidden):**
```json
{
  "error": "Sensor Unavailable",
  "message": "Sensor 'CO2-002' is currently under MAINTENANCE and cannot accept new readings.",
  "status": 403,
  "timestamp": 1710000000000
}
```

**Say:**
> "Sensor CO2-002 is seeded with status MAINTENANCE. Attempting to post a reading to it throws a SensorUnavailableException, mapped to 403 Forbidden. The error message clearly explains the problem without exposing any internal implementation detail."

---

### Step 12 — Error: GET Non-Existent Room → 404 (7:00 – 7:20)

**Postman Request:**
- Method: `GET`
- URL: `http://localhost:8080/api/v1/rooms/DOES-NOT-EXIST`

**Expected Response (404 Not Found):**
```json
{
  "error": "Not Found",
  "message": "Room with id 'DOES-NOT-EXIST' was not found.",
  "status": 404,
  "timestamp": 1710000000000
}
```

**Say:**
> "Missing resources return 404 with a consistent JSON error format. Every error in the API — whether 400, 403, 404, 409, 422, or 500 — follows the same error schema: error, message, status, timestamp."

---

### Step 13 — Logging and Architecture Brief (7:20 – 8:15)

**Show the terminal:** Scroll through the log output showing request/response pairs.

**Say:**
> "Every single request and response is logged by the ApiLoggingFilter. This class implements both ContainerRequestFilter and ContainerResponseFilter — it's registered once in AppConfig and runs automatically for every endpoint. No logging code exists in any resource method. This is the correct way to handle cross-cutting concerns in JAX-RS."

**Briefly show AppConfig.java:** Point out how all components are registered — resources, mappers, filters, Jackson feature — in one place.

**Briefly show InMemoryStore.java:** Point out `ConcurrentHashMap` and `CopyOnWriteArrayList` for thread safety.

**Say:**
> "The store uses ConcurrentHashMap and CopyOnWriteArrayList specifically because JAX-RS creates a new resource instance per request — multiple requests can be in-flight simultaneously. Thread-safe collections ensure no data corruption under concurrent access."

---

### Step 14 — Conclusion (8:15 – 9:00)

**Say:**
> "To summarise: I've built a complete Smart Campus REST API using JAX-RS and Jersey with no database. The API is versioned at /api/v1, handles all required CRUD operations, uses custom exception mappers for precise error responses — 409 for room conflict, 422 for linked resource issues, 403 for maintenance sensors — and logs all traffic via a container filter. No stack traces are ever exposed to clients. All data is held in thread-safe in-memory collections. Thank you."

---

## Tips for Recording

1. Speak at a measured pace — not too fast.
2. After each Postman response, pause briefly and point out the key part (status code, specific field, error message).
3. Keep the terminal visible — the log output reinforces the logging discussion.
4. Do not read code line by line — describe what each component does at a high level.
5. If you make a mistake in Postman, stay calm and correct it — it shows you understand what you are doing.
6. End exactly on or before 10 minutes.
