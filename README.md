# Smart Campus Sensor & Room Management API

## 1. Overview

This API lets campus facilities staff and automated building systems register rooms, deploy sensors in those rooms, and record sensor readings over time. It exposes four resource groups — rooms, sensors, readings, and a discovery root — over HTTP using JSON. Rooms track a name, capacity, and a list of sensor IDs. Sensors carry a type (e.g. `CO2`, `TEMPERATURE`), a status (`ACTIVE` or `MAINTENANCE`), and a `currentValue` field that is updated every time a new reading is posted. All data lives in memory; nothing is written to disk. The server runs as a standalone Grizzly process started from `Main.java`; there is no WAR file and no external servlet container needed.

---

## 2. Project Structure

```text
smart-campus-api/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── smartcampus/
                    ├── Main.java                        # Entry point — starts embedded Grizzly server
                    ├── SmartCampusApplication.java      # ResourceConfig — registers all packages
                    ├── DataStore.java                   # Static ConcurrentHashMaps for rooms, sensors, readings
                    ├── model/
                    │   ├── Room.java
                    │   ├── Sensor.java
                    │   └── SensorReading.java
                    ├── resource/
                    │   ├── DiscoveryResource.java       # GET /api/v1/
                    │   ├── RoomResource.java            # /api/v1/rooms
                    │   ├── SensorResource.java          # /api/v1/sensors  (sub-resource locator for readings)
                    │   └── SensorReadingResource.java   # /api/v1/sensors/{id}/readings
                    ├── exception/
                    │   ├── RoomNotEmptyException.java
                    │   ├── SensorUnavailableException.java
                    │   └── LinkedResourceNotFoundException.java
                    │   └── mapper/
                    │       ├── RoomNotEmptyExceptionMapper.java          # → 409
                    │       ├── SensorUnavailableExceptionMapper.java     # → 403
                    │       ├── LinkedResourceNotFoundExceptionMapper.java # → 422
                    │       └── GlobalExceptionMapper.java                # Throwable → 500
                    └── filter/
                        └── ApiLoggingFilter.java        # Logs method, URI, and response status for every request
```

---

## 3. Prerequisites

| Requirement | Minimum version |
|---|---|
| Java | 17 |
| Maven | 3.8 |

No external servlet container is needed. The project uses `jersey-container-grizzly2-http 3.1.5`, which embeds an HTTP server into the JAR.

---

## 4. Build & Run

Build a fat JAR (the `exec-maven-plugin` is already configured in `pom.xml`):

```bash
mvn clean package
```

Run the server:

```bash
mvn exec:java
```

The server starts at `http://localhost:8080/api/v1/` and prints:

```
Smart Campus API running at http://localhost:8080/api/v1/
Press Enter to stop...
```

Confirm the API is up:

```bash
curl http://localhost:8080/api/v1/
```

---

## 5. API Reference

### Discovery

| Method | Path | Description | Request Body | Success Response | Status Codes |
|---|---|---|---|---|---|
| `GET` | `/api/v1/` | Returns API version, contact, and links to `/rooms` and `/sensors` | None | `{"apiVersion":"v1","contact":"...","resources":{...}}` | 200 |

### Rooms — `/api/v1/rooms`

| Method | Path | Description | Request Body | Success Response | Status Codes |
|---|---|---|---|---|---|
| `GET` | `/api/v1/rooms` | Returns all rooms | None | Array of Room objects | 200 |
| `POST` | `/api/v1/rooms` | Creates a room | `{"id":"...","name":"...","capacity":0}` | `{"id":"..."}` | 201, 400 |
| `GET` | `/api/v1/rooms/{id}` | Returns a single room by ID | None | Room object | 200, 404 |
| `DELETE` | `/api/v1/rooms/{id}` | Deletes a room. Rejected if the room's `sensorIds` list is non-empty | None | Empty body | 204, 404, 409 |

### Sensors — `/api/v1/sensors`

| Method | Path | Description | Request Body | Success Response | Status Codes |
|---|---|---|---|---|---|
| `GET` | `/api/v1/sensors` | Returns all sensors. Pass `?type=CO2` to filter by type (case-insensitive) | None | Array of Sensor objects | 200 |
| `POST` | `/api/v1/sensors` | Creates a sensor. Throws `LinkedResourceNotFoundException` if `roomId` does not exist in `DataStore.rooms` | `{"id":"...","roomId":"...","type":"...","status":"..."}` | `{"id":"..."}` | 201, 400, 422 |

### Readings — `/api/v1/sensors/{id}/readings`

| Method | Path | Description | Request Body | Success Response | Status Codes |
|---|---|---|---|---|---|
| `GET` | `/api/v1/sensors/{id}/readings` | Returns all readings for the sensor | None | Array of SensorReading objects | 200, 404 |
| `POST` | `/api/v1/sensors/{id}/readings` | Adds a reading. Throws `SensorUnavailableException` if `sensor.status` equals `"MAINTENANCE"`. On success, sets `sensor.currentValue` to the new reading's value | `{"value":0.0}` | `{"id":"<uuid>"}` | 201, 403, 404 |

> **Side effect:** A successful POST to `/readings` updates the parent sensor's `currentValue` field in `DataStore.sensors`.

---

## 6. Sample curl Commands

All commands target `http://localhost:8080/api/v1`. Start the server with `mvn exec:java` before running them.

**Create a room**
```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Central Library Reading Room","capacity":120}'
```
Expected: `{"id":"LIB-301"}` — HTTP 201

---

**Create a sensor in room LIB-301**
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-007","roomId":"LIB-301","type":"CO2","status":"ACTIVE","currentValue":0.0}'
```
Expected: `{"id":"CO2-007"}` — HTTP 201. The sensor ID `"CO2-007"` is also added to `room.sensorIds`.

---

**Post a reading to an ACTIVE sensor (verify `currentValue` side effect)**
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/CO2-007/readings \
  -H "Content-Type: application/json" \
  -d '{"value":415.5}'
```
Expected: `{"id":"<uuid>"}` — HTTP 201. After this call, `GET /api/v1/sensors/CO2-007` will show `"currentValue":415.5`.

---

**Post a reading to a MAINTENANCE sensor → 403**
```bash
# First, create a sensor in maintenance status
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","roomId":"LIB-301","type":"TEMPERATURE","status":"MAINTENANCE","currentValue":0.0}'

# Then attempt a reading
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.1}'
```
Expected: `{"error":"Sensor is under maintenance."}` — HTTP 403

---

**Delete a room that still has sensors → 409**
```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```
Expected: `{"error":"Room contains active sensors."}` — HTTP 409. `RoomResource.deleteRoom` checks `room.getSensorIds().isEmpty()` before removing.

---

**Create a sensor with a non-existent roomId → 422**
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","roomId":"INVALID-101","type":"TEMPERATURE","status":"ACTIVE","currentValue":0.0}'
```
Expected: `{"error":"Referenced room does not exist."}` — HTTP 422

---

**Filter sensors by type**
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"
```
Expected: Array containing only sensors whose `type` matches `"CO2"` (case-insensitive). Returns an empty array `[]` if none match.

---

## 7. Report — Answers to Coursework Questions

### Q1 (Part 1.1): JAX-RS resource lifecycle and in-memory state

By default, JAX-RS creates a new instance of each resource class for every incoming request. This means you cannot store application data in instance fields — they would be created fresh and then thrown away after each response.

This project puts all data in `DataStore`, which declares its three maps as `public static final` fields:

```java
public static final Map<String, Room>   rooms    = new ConcurrentHashMap<>();
public static final Map<String, Sensor> sensors  = new ConcurrentHashMap<>();
public static final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();
```

Because the fields are `static`, they belong to the class, not to any one instance. Every request — regardless of which `RoomResource` or `SensorResource` object handles it — reads from and writes to the same map. `ConcurrentHashMap` is used instead of `HashMap` because multiple requests can arrive on different threads at the same time. `ConcurrentHashMap` allows concurrent reads and uses segment-level locking on writes, which prevents the data corruption and `ConcurrentModificationException` errors that would occur with a plain `HashMap` under concurrent load.

---

### Q2 (Part 1.2): HATEOAS and its benefit to client developers

HATEOAS means the server includes links in its responses that tell the client what it can do next, rather than the client having to know all URLs upfront. A response to `POST /rooms` that follows HATEOAS would include a `self` link, a link to the room's sensors, and a link to delete the room. The client follows those links instead of constructing URLs from a separate document.

The practical benefit is that URLs can change server-side without breaking clients. A client that navigates links does not hardcode `/api/v1/rooms/LIB-301` — it discovers the path at runtime from the response. This is especially useful when resources move, are versioned, or have conditional links: for example, a DELETE link would only appear if the room has no sensors, communicating that precondition to the client without requiring it to call a separate endpoint.

This API's discovery endpoint (`GET /api/v1/`) returns a `resources` map with paths to `/api/v1/rooms` and `/api/v1/sensors`, which is a step toward HATEOAS. Individual resource responses do not yet include navigable links, so the implementation sits at Richardson Maturity Model Level 2.

---

### Q3 (Part 2.1): Returning IDs vs. full objects in list responses

`GET /api/v1/rooms` currently returns full `Room` objects, including their `sensorIds` list. The alternative — returning only IDs — reduces response size but forces the client to make one `GET /api/v1/rooms/{id}` call per room to get usable data. That's the N+1 problem: 50 rooms means 51 HTTP requests instead of one.

Returning full objects is the right choice when the client needs room data to render a list (e.g. a table of room names and capacities). It means one round trip regardless of how many rooms there are.

Returning only IDs makes sense when the list is very large and most entries won't be displayed in detail, or when the client applies client-side filtering before fetching full records. In that case, transferring 10 KB of full objects to use 500 bytes of IDs wastes bandwidth.

For this project, where rooms are few and always displayed in full, returning the complete object per room is the correct choice. If the API needed to support paginated lists of thousands of rooms, a cursor-based response returning minimal fields (ID, name) with a separate detail endpoint would be more appropriate.

---

### Q4 (Part 2.2): Is DELETE idempotent?

HTTP DELETE is defined as idempotent: sending the same request multiple times must leave the server in the same final state. In this implementation, the first `DELETE /api/v1/rooms/LIB-301` call removes the room from `DataStore.rooms` and returns 204. A second identical call finds no entry in the map and returns 404.

The server state after both calls is identical — the room does not exist. That satisfies idempotency. The response code changes from 204 to 404, but idempotency is defined on server state, not on the response code.

One edge case in this implementation: if the room has sensors, `deleteRoom` throws `RoomNotEmptyException` before removing anything, returning 409. In that case the server state does not change, which is also idempotent — calling it repeatedly gives 409 each time until the sensors are removed.

---

### Q5 (Part 3.1): `Content-Type: text/plain` on a `@Consumes(APPLICATION_JSON)` endpoint

`@Consumes(MediaType.APPLICATION_JSON)` on the endpoint is a contract declaration. When a POST arrives with `Content-Type: text/plain`, the JAX-RS runtime checks the incoming `Content-Type` header against all registered `@Consumes` values before handing the request to the method.

No match is found, so the runtime never calls the resource method. It returns HTTP 415 (Unsupported Media Type) directly. The request body is not read and no deserialization is attempted.

This happens at the framework routing layer, not in application code, so there is no risk of a `text/plain` body reaching Jackson or causing a parse exception inside the method. The 415 response is generated by Jersey itself.

---

### Q6 (Part 3.2): `@QueryParam` vs. path segments for filtering

A URL path identifies a resource. `/sensors/type/CO2` says "CO2 is a resource at this location," which is wrong — CO2 is a filter criterion, not a resource. That design also requires a new route for every new filter dimension. Adding a status filter would need `/sensors/type/CO2/status/ACTIVE` or a separate route, and the routing table grows with every new filter.

`@QueryParam` keeps the path clean: `/sensors` identifies the collection, `?type=CO2` narrows it. Adding a second filter is `/sensors?type=CO2&status=ACTIVE` with no route change. Clients can omit query parameters entirely to get the full collection, which is what this implementation does — `GET /api/v1/sensors` with no `type` param returns all sensors, and the filter is applied only when the param is present.

Query parameters also compose naturally with pagination (`?page=2&size=20`) and sorting (`?sort=type`) without any changes to the path.

---

### Q7 (Part 4.1): Sub-resource locator pattern

In `SensorResource`, the readings path is handled by this method:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

There is no `@GET` or `@POST` on this method. JAX-RS sees it as a sub-resource locator: it returns an object (`SensorReadingResource`) and JAX-RS continues dispatching the request to that object's annotated methods. The `sensorId` path parameter is passed to `SensorReadingResource` via its constructor, so the reading resource always knows which sensor it belongs to.

The alternative is to define every `GET /sensors/{id}/readings` and `POST /sensors/{id}/readings` method directly inside `SensorResource`. That works for two methods, but becomes a problem when readings have their own sub-paths, when the reading logic is complex, or when you want to test `SensorReadingResource` in isolation. The locator pattern puts reading logic in its own class with a single responsibility, and keeps `SensorResource` focused on sensor-level operations.

---

### Q8 (Part 5.2): HTTP 422 vs. 404 for a missing `roomId` reference

404 means the URL the client requested does not exist. When a client posts to `/api/v1/sensors`, that URL exists and is handled correctly. The problem is not the URL — it's that the body contains a `roomId` value that does not match any room in `DataStore.rooms`.

422 (Unprocessable Entity) means the server understood the request format and the URL is valid, but the content fails a business rule check. In this case the rule is: a sensor must reference an existing room. The body is syntactically valid JSON with the right fields; it just fails the referential check.

Using 404 would tell the client the endpoint does not exist, which is false and would mislead them into thinking they have the wrong URL. Using 422 tells them exactly what's wrong: the data is well-formed but logically inconsistent. This makes the error actionable — the client knows to fix the `roomId` in the request body, not the URL.

---

### Q9 (Part 5.4): Security risks from exposing raw stack traces

A stack trace names every class and method in the call chain, including internal package names (e.g. `com.smartcampus.resource.SensorResource.createSensor`), the Jersey and Grizzly versions, and sometimes the operating system path to the JAR file. An attacker can use this to:

1. **Identify library versions.** If the trace shows `jersey-server 3.1.5`, the attacker checks whether that version has known CVEs and targets them directly.
2. **Map the internal structure.** Package names reveal the application's class layout, making it easier to guess other endpoints or identify which methods to fuzz.
3. **Confirm injection payloads worked.** If a SQL injection or deserialization attack partially succeeds, the resulting exception trace tells the attacker which layer broke and how to adjust.

This project's `GlobalExceptionMapper` catches `Throwable` and returns only `{"error":"Unexpected error occurred"}` with a 500 status. The exception message and stack trace are never written to the response body. The full trace is still logged server-side (via `java.util.logging`), where it is visible to developers but not to API consumers.

---

### Q10 (Part 5.5): JAX-RS filters for logging vs. per-method calls

`ApiLoggingFilter` implements both `ContainerRequestFilter` and `ContainerResponseFilter`. Every request passes through it, so every HTTP call is logged — the HTTP method, the full request URI on the way in, and the response status code on the way out — without any code in the resource classes.

If you put `Logger.info(...)` calls inside each resource method instead, you log the inside of the method but not unmatched routes (which return 404 before any method runs), not requests rejected for wrong `Content-Type` (which return 415 without entering the method), and not framework-generated responses. You also have to remember to add logging to every new method you write.

The filter approach logs everything at the container boundary. It also separates the concern of monitoring from the concern of business logic: `RoomResource.createRoom` does not need to know anything about logging, and the logging format can be changed in one file without touching any resource class.

---

## 8. Known Limitations

### No persistence
All data is stored in `DataStore`'s static `ConcurrentHashMap` fields. When the server stops, all rooms, sensors, and readings are lost. There is no database, no file write, and no serialisation to disk.

### No authentication
Every endpoint is open. Any client that can reach port 8080 can create, read, or delete any resource.

### No concurrency control on compound checks
`ConcurrentHashMap` makes individual map operations thread-safe, but compound sequences — such as checking whether a room has sensors and then deleting it — are not atomic. Under concurrent load, it is possible for two threads to pass the emptiness check simultaneously and both proceed to delete.

### No sensor GET by ID
`SensorResource` has no `@GET @Path("/{id}")` method. Clients can list all sensors or filter by type, but cannot fetch a single sensor directly by its ID.

### Readings list is unbounded
Every POST to `/sensors/{id}/readings` appends to an `ArrayList` stored in `DataStore.readings`. There is no maximum size, no expiry, and no pagination. Long-running servers will accumulate readings indefinitely until the JVM runs out of heap memory.
