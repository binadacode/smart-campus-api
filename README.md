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
                    │   ├── SensorRoom.java              # /api/v1/rooms
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

## 4. Build & Run (NetBeans)

This project is configured for deployment via the NetBeans IDE and Apache Tomcat. You do not need to use terminal commands to launch the API; NetBeans handles the Maven build and Tomcat deployment automatically.

**Step 1: Build the Project**
1. In the **Projects** window on the left, right-click the `smart-campus-api` project folder.
2. Select **Clean and Build**.
3. Watch the Output window at the bottom of the screen and wait for the green `BUILD SUCCESS` message.

**Step 2: Run the Server**
1. Right-click your project folder again and select **Run** (or click the green "Play" button in the top toolbar).
2. NetBeans will package your application and deploy it to your configured Apache Tomcat server.
3. Check the **Apache Tomcat** tab in the Output window. The server is ready when you see:
   `Smart Campus API Initialized at /api/v1`

**Step 3: Confirm the API is up**
To verify the server is actively listening, open Postman and send a basic `GET` request to the discovery endpoint:
```text
GET http://localhost:8080/api/v1/

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

> **Note:** The server generates all resource IDs as UUIDs server-side. Any `id` field sent in the request body is ignored. The examples below use shell variables to capture returned IDs for chaining commands.

**Create a room**
```bash
ROOM_ID=$(curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name":"Central Library Reading Room","capacity":120}' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Room ID: $ROOM_ID"
```
Expected response body: `{"id":"<uuid>"}` — HTTP 201

---

**Create a CO2 sensor in that room**
```bash
SENSOR_ID=$(curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"roomId\":\"$ROOM_ID\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0}" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Sensor ID: $SENSOR_ID"
```
Expected response body: `{"id":"<uuid>"}` — HTTP 201. The sensor UUID is also appended to `room.sensorIds`.

---

**Post a reading to an ACTIVE sensor (verify `currentValue` side effect)**
```bash
curl -s -X POST "http://localhost:8080/api/v1/sensors/$SENSOR_ID/readings" \
  -H "Content-Type: application/json" \
  -d '{"value":415.5}'
```
Expected: `{"id":"<uuid>"}` — HTTP 201. After this call, `GET /api/v1/sensors/$SENSOR_ID` will show `"currentValue":415.5`.

---

**Post a reading to a MAINTENANCE sensor → 403**
```bash
# Create a sensor in MAINTENANCE status in the same room
MAINT_ID=$(curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"roomId\":\"$ROOM_ID\",\"type\":\"TEMPERATURE\",\"status\":\"MAINTENANCE\",\"currentValue\":0.0}" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

# Then attempt a reading — this will be rejected
curl -s -X POST "http://localhost:8080/api/v1/sensors/$MAINT_ID/readings" \
  -H "Content-Type: application/json" \
  -d '{"value":22.1}'
```
Expected: `{"message":"Sensor '<uuid>' is under maintenance and cannot accept new readings."}` — HTTP 403

---

**Delete a room that still has sensors → 409**
```bash
curl -s -X DELETE "http://localhost:8080/api/v1/rooms/$ROOM_ID"
```
Expected: `{"message":"Room contains active sensors."}` — HTTP 409. `SensorRoom.deleteRoom` checks `room.getSensorIds().isEmpty()` before removing.

---

**Create a sensor with a non-existent roomId → 422**
```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"roomId":"INVALID-101","type":"TEMPERATURE","status":"ACTIVE","currentValue":0.0}'
```
Expected: `{"message":"Room with ID 'INVALID-101' does not exist."}` — HTTP 422

---

**Filter sensors by type**
```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"
```
Expected: Array containing only sensors whose `type` matches `"CO2"` (case-insensitive). Returns an empty array `[]` if none match.

---

## 7. Report — Answers to Coursework Questions

### Q1 (Part 1.1): JAX-RS resource lifecycle and in-memory state

JAX-RS creates a new instance of each resource class for very incoming request by default. This means application data cannot be stored in instance fields, they would be created fresh and then thrown away after each response. While JAX-RS also supports a **Singleton** lifecycle (where a single instance handles all requests), this project intentionally uses the default **Request-scoped** lifecycle to maintain strict request isolation. This project puts all data in DataStore, which declares its 3 maps as public static final fields:

```java
public static final Map<String, Room>   rooms    = new ConcurrentHashMap<>();
public static final Map<String, Sensor> sensors  = new ConcurrentHashMap<>();
public static final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();
```

As the fields are static they belong to the class and not to any 1 instance. Every request reads from and writes to the same map regardless of which object handles it. 

---

### Q2 (Part 1.2): HATEOAS and its benefit to client developers

HATEOAS means the server includes links in its responses that tell the client what it can do next instead of the client knowing all URLs upfront. A response to POST /rooms that follows HATEOAS would include a self link, a link to the room’s sensors and a link to delete the room. The client uses those links rather than creating URLs on a different document. The usefulness of this in practice is that URLs can be modified on the server side without errors. A client that navigates links does not hardcode /api/v1/rooms/LIB-301, it discovers the path at runtime from its response. This is particularly useful when resources move, are versioned or have conditional links. The API’s discovery endpoint (GET /api/v1 ) returns a resources map with paths to /api/v1/rooms and /api/v1/sensors which is a step towards HATEOAS. 

---

### Q3 (Part 2.1): Returning IDs vs. full objects in list responses

GET /api/v1/rooms currently returns full Room objects including their sensorIds list. The other option is to return only IDs. This reduces response size but forces the client to make one GET /api/v1/rooms/{id} call per room to get usable data which is the N+1 problem. Full objects should be returned in cases where the client requires room data to render a list. It includes a round trip. The simplest decision is to send back only the IDs when the list is extremely large and most of the entries will not be shown at detail, or when the client is going to filter the list with client-side filtering before retrieving the full records. In that case transferring full objects to use IDs wastes bandwidth. For this project where rooms are few and always displayed in full returning the complete objects per room is the correct choice. 

---

### Q4 (Part 2.2): Is DELETE idempotent?

HTTP DELETE is defined as idempotent. In this implementation the first DELETE /api/v1/rooms/LIB-301 call removes the room from DataStore.rooms and returns 204. A 2nd identical call finds no entry in the map and returns 404. The server state after both calls is identical (the room does not exist) which satisfies idempotency. 

---

### Q5 (Part 3.1): `Content-Type: text/plain` on a `@Consumes(APPLICATION_JSON)` endpoint

@Consumes(MediaType.APPLICATION_JSON) on the endpoint is a contract declaration. In the case of a POST with Content-Type: text/plain the JAX-RS runtime verifies the receiving Content-Type header against all registered values of the type of content and then passes the request to the method. This occurs at the framework routing layer and does not occur within application code hence there is no possibility of text/plain body getting to Jackson resulting in a parse exception within the method. The 415 response is generated by Jersey itself.

---

### Q6 (Part 3.2): `@QueryParam` vs. path segments for filtering

A URL path identifies a resource. That design requires a new route for every new filter dimension. Adding a status filter would need a separate route and the routing table would grow with every new filter. @QueryParam keeps the path clean. A second filter can be added with no route change. Clients can remove query parameters completely to get the full collection which is what this implementation does. Query parameters also combine naturally with pagination and sorting without any changes to the path.

---

### Q7 (Part 4.1): Sub-resource locator pattern

The readings path is handled by the below method in SensorResource:

```java
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);
}
```

There is no @GET or @POST on this method. JAX-RS recognizes it as a subresource locator because it returns an object (SensorReadingResource). JAX-RS continues dispatching the request to that object’s annotated methods. The sensorId path parameter is passed to SensorReadingResource through its constructor so the reading resource always knows which sensor it belongs to. The alternative is to define every GET /sensors/{id}/readings and POST /sensors/{id}/readings method directly inside SensorResource. That works for 2 methods but becomes a problem when readings have their own sub paths, when the reading logic is complicated or when SensorReadingResource should be tested separately. The locator pattern puts reading logic in its own class with a single responsibility and keeps SensorResouce focused on sensor level operations.

---

### Q8 (Part 5.2): HTTP 422 vs. 404 for a missing `roomId` reference

404 means the URL the client requested does not exist. When a client posts to /api/v1/sensors that URL exists and is handled correctly. 422 (unprocessable entity) means the server understood the request format and URL is valid but the content fails a business rule check. In this case the rule is a sensor must reference an existing room. Using 404 would tell the client the endpoint does not exist which is wrong as it would lead them into thinking they have the wrong URL. Using 422 tells the client exactly that the data is well formed but logically inconsistent. This makes the error actionable as the client will know to fix the roomId in the request body not the URL.

---

### Q9 (Part 5.4): Security risks from exposing raw stack traces

A stack trace names every class and method in the call chain, including package names, the Jersey and Grizzly versions and sometimes the operating system path to the JAR file. An attacker can use this to:

1. **Identify library versions:** if the trace shows the Jersey version the attacker can check whether that version has known CVEs and target them.
2. **Map the internal structure:** package names reveal the application’s class layout. This makes it easier to guess other endpoints or identiy which methods should be attacked.
3. **Confirm injection payloads worked:** if an SQL injection or deserialization attack partially succeeds, the resulting exception trace tells the attacker which layer broke and how to adjust accordingly.

This project’s GlobalExceptionMapper catches Throwable and returns only {"error":"Unexpected error occurred"} with a 500 status. The exception message and stack trace are never written to the response body. The full trace is still logged server side where developers can view it but customers cannot.

---

### Q10 (Part 5.5): JAX-RS filters for logging vs. per-method calls

ApiLoggingFilter implements both ContainerRequestFilter and ContainerResponseFilter. Every request passes through it so every HTTP call is logged including the HTTP method, full request URI on the way in and response status code on the way out without any code in the resource classes. The filter approach logs everything at the container boundary. Also it supports separation of concerns as monitoring is separated from the business logic. RoomResource.createRoom does not need to know anything about logging and the logging format can be changed in one file without touching any resource class.

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
