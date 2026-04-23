# Smart Campus API — Report Answers
**Module:** 5COSC022W — Client-Server Architectures  
**Academic Year:** 2025/26

---

> This document contains only the written answers to the coursework conceptual questions,
> grouped by part. It is intended to be pasted into the PDF report submission.

---

## Part 1 — JAX-RS Resource Lifecycle and HATEOAS

### Q1.1 — Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created per request or is it a singleton by default?

JAX-RS follows a **per-request lifecycle** by default. The JAX-RS runtime creates a brand-new instance of a resource class for each incoming HTTP request and destroys it once the response has been sent. This is the behaviour specified in the JAX-RS specification (Jakarta RESTful Web Services 3.1, §3.1) and is the default when no explicit `@Singleton` or custom lifecycle annotation is applied.

The rationale for per-request instantiation is thread safety: each request thread operates on its own object, so there is no risk of instance-level fields being mutated concurrently by separate requests.

### Q1.2 — How does the per-request lifecycle affect managing shared in-memory data structures?

Because each resource class instance is freshly created per request, **instance fields cannot be used to hold shared application state**. Any data stored in an instance field is visible only to that single request and is garbage-collected once the response returns.

In this project, all shared state (rooms, sensors, readings) is held in `InMemoryStore`, a singleton accessed via `InMemoryStore.getInstance()`. Each resource class instance acquires a reference to this shared singleton on construction. This ensures that all requests, regardless of which resource instance handles them, read from and write to the same underlying `ConcurrentHashMap` and `CopyOnWriteArrayList` collections, making the in-memory store the effective "database" of the application.

Without this pattern — if the collections were stored as resource instance fields — each request would start with an empty data set and changes made by one request would be invisible to all others.

### Q1.3 — Why is HATEOAS (Hypermedia as the Engine of Application State) valuable? How does it help clients compared to static documentation?

HATEOAS is the principle that API responses should include hyperlinks to related resources and available next actions, rather than forcing clients to construct URLs from external documentation. A response from `POST /rooms` might include links to `GET /rooms/{id}` and `GET /rooms/{id}/sensors`, guiding the client through valid transitions.

**Value for clients:** Clients that navigate via embedded links are decoupled from URL structure. If the server changes a URL pattern (e.g., from `/rooms/{id}/sensors` to `/rooms/{id}/devices`), a HATEOAS client that follows links rather than hardcoding paths will adapt without modification, as long as the link relation names remain stable.

**Compared to static documentation:** Static API documentation goes stale and clients must be manually updated when URLs change. HATEOAS makes the API self-describing at runtime — the server communicates current state and valid next steps dynamically. This is particularly valuable in long-lived integrations where client and server evolve independently.

In this implementation, the discovery endpoint (`GET /api/v1`) returns `resources.rooms` and `resources.sensors` links, providing a basic HATEOAS entry point. The `Location` header returned on every `201 Created` response is also a form of hypermedia, pointing the client directly to the newly created resource.

---

## Part 2 — Room List Design and DELETE Idempotency

### Q2.1 — IDs only versus full room objects when returning room lists: discuss the bandwidth and client-side processing trade-offs.

**Returning IDs only** (`["LIB-301", "LAB-101", "HALL-A"]`):
- Minimal bandwidth per response
- Client must issue one additional `GET /rooms/{id}` request per room to retrieve details — this is the "N+1 problem"
- Appropriate when the collection is very large and the client only needs to select a specific item before fetching details
- Forces more round trips and increases perceived latency

**Returning full objects** (`[{"id":"LIB-301","name":"Library Quiet Study","capacity":60,...}, ...]`):
- Higher per-response payload size
- Client can render a full list in a single round trip
- No additional network calls required for typical use cases such as a dashboard listing all rooms
- For a campus system with a manageable number of rooms, this is the correct default choice

**Design decision in this implementation:** `GET /rooms` returns full `Room` objects. For a campus-scale system this is appropriate. If the system were to grow to thousands of rooms, the correct solution would be to introduce pagination (`?page=0&size=20`) and return full objects per page, rather than switching to ID-only responses.

### Q2.2 — Is DELETE idempotent in this implementation? Explain the behaviour on repeated identical DELETE requests.

**Yes, DELETE is idempotent** in this implementation, in conformance with RFC 9110 (HTTP Semantics).

Idempotency means that making the same request N times produces the same *server state* as making it once. The definition does not require identical status codes across repeated calls.

Behaviour in this implementation:
- **First DELETE on an existing empty room** → `204 No Content`. The room is removed from the store.
- **Second DELETE on the same (now missing) room** → `404 Not Found`. The store has no record of the room.

After the first call, the server state is "room does not exist." After the second call, the server state is still "room does not exist." The state is unchanged, confirming idempotency.

Returning `404` on subsequent calls is the more informative choice: it signals that the resource is genuinely absent rather than silently pretending the deletion succeeded again. Some API designs return `204` on repeated calls to enforce strict idempotency of status codes, but the HTTP specification explicitly permits `404` here, and it is widely accepted practice.

---

## Part 3 — Content Negotiation and Query Parameter Filtering

### Q3.1 — What happens if a client sends Content-Type: text/plain or application/xml to a JSON-only POST endpoint?

The POST endpoints in this API are annotated with `@Consumes(MediaType.APPLICATION_JSON)`. This annotation tells the JAX-RS runtime (Jersey) to match the method only when the incoming request carries `Content-Type: application/json`.

If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, Jersey will fail to find a matching resource method for that media type combination and will automatically respond with **HTTP 415 Unsupported Media Type**. The resource method is never invoked.

This behaviour is part of the JAX-RS content negotiation mechanism and requires no custom code. It protects the endpoint from receiving data in formats the application is not equipped to parse, and it communicates clearly to the client that they must correct their `Content-Type` header.

### Q3.2 — Why is query parameter filtering better than path-based filtering for collection searches?

Path-based filtering would look like `GET /sensors/CO2`, implying that `CO2` is a named sub-resource of sensors — a permanent identifier in the URL hierarchy. This is semantically incorrect: `CO2` is not a resource, it is a *filter criterion* applied to the sensors collection.

Query parameters (`GET /sensors?type=CO2`) are the semantically correct mechanism for filtering, sorting, and paginating collections because:

1. **They do not change the resource identity.** The resource is `/sensors`. The query string modifies *how* that resource is returned, not *which* resource is being addressed.
2. **They are optional by convention.** `GET /sensors` and `GET /sensors?type=CO2` both refer to the sensors collection, with the latter applying a filter. Path segments cannot be optional without complex routing rules.
3. **They are composable.** Multiple filters can be chained: `?type=CO2&status=ACTIVE`. Path-based filtering does not compose gracefully.
4. **They align with REST and HTTP conventions** used by all major public APIs (e.g., GitHub, Stripe, Twitter).

Using path-based filtering for search/filter operations is a common anti-pattern that pollutes the URL hierarchy and misrepresents the relationship between resources.

---

## Part 4 — Sub-Resource Locator Pattern

### Q4.1 — What are the benefits of the sub-resource locator pattern?

A **sub-resource locator** is a JAX-RS resource method that, rather than processing a request and returning a `Response`, returns an object instance that will handle the remainder of the path. In this implementation, the `getReadingsResource(@PathParam("sensorId") String sensorId)` method in `SensorResource` returns a `SensorReadingResource` instance, delegating all `/sensors/{sensorId}/readings` traffic to it.

Benefits:

1. **Encapsulation** — reading-related logic (validation, CRUD on readings, updating `currentValue`) is fully contained within `SensorReadingResource`. `SensorResource` has no knowledge of reading internals.

2. **Single Responsibility Principle** — each class has one clear job: `SensorResource` manages sensors, `SensorReadingResource` manages readings for a given sensor.

3. **Testability** — `SensorReadingResource` can be instantiated directly in unit tests with any `sensorId` string, without going through the full JAX-RS routing chain.

4. **Scalability of design** — if the readings sub-resource grows in complexity (e.g., aggregations, filtering by date range), it can evolve independently without touching `SensorResource`.

5. **Contextual injection** — the `sensorId` is passed at construction time, giving `SensorReadingResource` the context it needs without relying on `@PathParam` injection (which is unavailable in sub-resource classes that are not registered as root resources).

### Q4.2 — Why is the sub-resource locator pattern better than putting all nested paths in one giant controller class?

A single class handling all of `/sensors`, `/sensors/{id}`, and `/sensors/{id}/readings` would grow rapidly as each sub-path requires its own methods, validations, and state checks. The resulting class would violate the Single Responsibility Principle and become difficult to read, test, and maintain.

The sub-resource locator enforces a natural **hierarchical decomposition** that mirrors the URL structure:
- `SensorResource` owns `/sensors/**`
- `SensorReadingResource` owns `/sensors/{id}/readings/**`

This mirrors good object-oriented design: just as a `Room` object in a domain model should not know how to manage `SensorReading` persistence, the sensor-level resource controller should not contain reading-level operations. The pattern makes the code structure self-explanatory to any reader.

---

## Part 5 — 422 vs 404, Stack Trace Security, and Filter-Based Logging

### Q5.1 — Why can 422 Unprocessable Entity be more semantically correct than 404 Not Found when a linked resource is missing inside a valid JSON payload?

The distinction is between a **routing error** and a **business logic error**.

- **HTTP 404 Not Found** means the URL the client requested does not correspond to any resource on the server. The client has navigated to the wrong location.

- **HTTP 422 Unprocessable Entity** means the request reached the correct endpoint, was syntactically valid (correct JSON structure, correct `Content-Type`), and was understood by the server — but the *semantic content* of the request is invalid because it references an entity that does not exist.

In the case of `POST /sensors` with a `roomId` of `"GHOST-ROOM"`: the URL `/api/v1/sensors` is correct (it exists), the JSON is well-formed, and the `Content-Type` is correct. The problem is that `GHOST-ROOM` does not exist in the rooms store — a referential integrity failure. Returning 404 here would mislead the client into thinking they called the wrong URL. Returning 422 clearly communicates: "Your request was valid and we understood it, but we cannot process it because of a semantic problem in your data."

This is more precise, more actionable, and more honest from an HTTP semantics perspective.

### Q5.2 — Why is exposing Java stack traces in API responses a cybersecurity risk? What can an attacker learn?

A stack trace exposes multiple layers of internal implementation detail:

1. **Framework and library versions** — e.g., `org.glassfish.jersey.server.internal...` reveals Jersey and its version. Attackers cross-reference this with public CVE databases to identify known exploits.

2. **Internal package and class structure** — reveals the architecture of the application, making it easier to craft targeted attacks.

3. **File paths** — can reveal the server's directory structure, OS, and deployment conventions.

4. **Logic flow** — shows which code paths were executed, potentially hinting at injection points or unhandled edge cases.

5. **Third-party library names** — reveals the full dependency chain, enabling supply-chain attack research.

OWASP lists "Security Misconfiguration" as a top-ten web application vulnerability, and stack trace leakage is a classic example. In this project, `GlobalExceptionMapper` logs the full exception server-side (where only authorised developers can see it) and returns only a generic, safe `500` message to the client, fully preventing information leakage.

### Q5.3 — Why are filters better than Logger.info() inside every method for cross-cutting concerns like logging?

Embedding `Logger.info(...)` calls directly in every resource method has the following problems:

1. **Repetition** — the same logging code must be written in dozens of methods.
2. **Inconsistency** — different developers may log different fields in different formats.
3. **Maintenance burden** — changing the log format requires touching every logged method.
4. **Violation of SRP** — resource methods should contain only business logic. Logging is infrastructure.
5. **Risk of omission** — a developer adding a new endpoint might forget to add logging.

A `ContainerRequestFilter` / `ContainerResponseFilter` is registered once in `AppConfig` and executes automatically for every request and response, regardless of which resource method handles them. This is the **Aspect-Oriented Programming** (AOP) approach to cross-cutting concerns: the logging behaviour is defined in one place, applied universally, and completely invisible to business logic classes. If the log format must change, one class is updated.

This is the same principle behind authentication filters, CORS headers, and compression middleware in production APIs: concerns that apply across all endpoints belong in filters, not in resource methods.

---

*End of Report Answers*
