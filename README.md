# Smart Campus Sensor & Room Management API

## 1. Overview
The Smart Campus API is a lightweight RESTful web service engineered to centrally orchestrate room availability and heterogeneous environmental sensor networks across existing university infrastructure. Targeting campus facilities management systems and automated building administration services, this API exposes endpoints for registering physical room dimensions, deploying arrayed diagnostic sensors (predominantly CO2 and thermal monitors), and systematically aggregating time-series telemetric readings. Designed using Java development paradigms alongside the JAX-RS specification, it employs in-memory data structures mapping highly optimized transactional requests traversing standardized HTTP network schemas to ensure maximum continuous operational interoperability.

## 2. Project Structure
```text
smart-campus-api
├── pom.xml
└── src
    └── main
        └── java
            └── com
                └── campus
                    ├── config/       # JAX-RS Application configuration overriding classes
                    ├── exception/    # Specialized custom exception domain definitions
                    │   └── mapper/   # JAX-RS exception mappers mapping errors to HTTP codes
                    ├── filter/       # Intercepting filters tracking cross-cutting telemetry via logging
                    ├── model/        # Application domain logic entities (Room, Sensor, Reading)
                    ├── resource/     # JAX-RS primary endpoint controllers and sub-resource locators
                    └── service/      # In-memory storage components processing HashMaps (DataStore)
```

## 3. Prerequisites
- **Java**: Java 17 or higher
- **Build Tool**: Maven 3.8+
- **Environment**: A compliant Servlet Container implementing Jakarta/Java EE constraints (e.g., Apache Tomcat 10, GlassFish, Eclipse Jetty)

## 4. Build & Run

To compile the underlying target modules and successfully package the deployment asset:
```bash
mvn clean package
```

Subsequently, automatically execute the compiled module natively using cargo plugins locally bridging underlying container structures:
```bash
# Execute local runtime configuration running packaged target components
mvn clean verify cargo:run
```

To explicitly verify the base URL and API response, query the discovery root endpoint:
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1
```

## 5. API Reference

### Discovery
| Method | Path | Description | Request Body | Success Response | Status Codes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| GET | `/api/v1` | Discovery endpoint evaluating version info, system contact, and application mappings | None | API metadata and root references | 200 |

### Rooms
| Method | Path | Description | Request Body | Success Response | Status Codes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| GET | `/api/v1/rooms` | Retrieves total registered room entities | None | Array of Room structures | 200 |
| POST | `/api/v1/rooms` | Registers discrete initialized room models | `{ "id": "...", "name": "...", "capacity": 0 }` | `{ "id": "" }` | 201 |
| GET | `/api/v1/rooms/{id}` | Fetches explicit target room entity definitions | None | Room parameters mapping identifier | 200, 404 |
| DELETE | `/api/v1/rooms/{id}` | Completely nullifies room identity and mapping reference | None | Empty response output | 204, 404, 409 |

### Sensors
| Method | Path | Description | Request Body | Success Response | Status Codes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| GET | `/api/v1/sensors` | Retrieves filtered aggregated sensor networks using `?type=` | None | Array containing mapped Sensors | 200 |
| POST | `/api/v1/sensors` | Injects valid new sensor devices establishing room relation | `{ "id": "...", "roomId": "...", "type": "...", "status": "..." }` | `{ "id": "..." }` | 201, 422 |
| GET | `/api/v1/sensors/{id}` | Retreives unique system device configuration schemas | None | Defined Sensor components | 200, 404 |

### Readings
| Method | Path | Description | Request Body | Success Response | Status Codes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| POST | `/api/v1/sensors/{id}/readings` | Submits continuous telemetric environmental readings | `{ "value": 0.0, "timestamp": "ISO-8601" }` | `{ "id": "..." }` | 201, 404, 403, 422 |

## 6. Sample curl Commands

Create a room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "LIB-301", "name": "Central Library", "capacity": 150}'
```

Create a sensor (valid roomId)
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "CO2-007", "roomId": "LIB-301", "type": "CO2", "status": "ACTIVE"}'
```

Add a reading to an ACTIVE sensor (verify currentValue side effect)
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-007/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 415.5, "timestamp": "2026-04-17T12:00:00Z"}'
```

Attempt a reading on a MAINTENANCE sensor → expect 403
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 22.1, "timestamp": "2026-04-17T12:00:00Z"}'
```

Delete a room that still has sensors → expect 409
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```

Create a sensor with a non-existent roomId → expect 422
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-999", "roomId": "INVALID-101", "type": "TEMPERATURE", "status": "ACTIVE"}'
```

Filter sensors by type: GET /api/v1/sensors?type=CO2
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"
```

## 7. Report — Answers to Coursework Questions

### Q1 (Part 1.1): What is the default lifecycle of a JAX-RS resource class — per-request or singleton? How does this affect managing in-memory data structures to avoid race conditions?
The default lifecycle of a JAX-RS resource class in standard implementations like Jersey is strictly per-request. The servlet container intrinsically instantiates a completely separate, fresh object of the target resource class upon receiving any valid HTTP request, processes the execution pipeline, and marks that instance for aggressive garbage collection immediately after the HTTP response payload gets dispatched to the client. This per-request paradigm directly dictates the architecture of any in-memory data persistence strategy. Given that multiple HTTP connections can execute concurrently on different threads while instantiating isolated resource objects, the underlying data layer must employ globally scoped references, typically achieved through static fields or dependency injection yielding Application-scoped singletons. Consequently, developers must rigorously reject standard Java collections, such as `java.util.HashMap` or `java.util.ArrayList`, since they lack intrinsic thread-safety and will rapidly produce unpredictable memory corruption or `ConcurrentModificationException` failures under typical load. Instead, engineers must integrate concurrent data structures like `java.util.concurrent.ConcurrentHashMap` or enforce strict synchronization primitives to securely isolate critical mutative blocks, ensuring absolute memory coherence and circumventing race conditions when multiple consumers execute overlapping `POST`, `PUT`, or `DELETE` requests traversing identical map segments.

### Q2 (Part 1.2): Why is HATEOAS considered a hallmark of advanced REST design? How does it benefit client developers over static documentation?
HATEOAS (Hypermedia As The Engine Of Application State) elevates the traditional client-server HTTP integration paradigm strictly towards Level 3 of the Richardson Maturity Model, representing the pinnacle of completely normalized RESTful architecture. At its core, HATEOAS requires that the backend server dynamically constructs and injects navigable hypermedia links structurally alongside the requested entity data payload. This fundamental paradigm shift successfully decouples the consuming application from rigidly mapped external URI path structures, dynamically projecting allowable state transitions directly as functional elements. Instead of executing API traversals based completely upon rigidly hardcoded endpoints transcribed from static external Swagger specifications or manual documentation, consuming developers employ the provided hypermedia payloads to dictate operational boundaries. This mechanism drastically benefits API client developers by allowing the backend application to alter existing URL paths or append new domain-driven interactions without irreparably breaking existing frontend or mobile consumers. Furthermore, HATEOAS enforces self-descriptive operational boundaries; if the authenticated client does not hold structural permission to manipulate a resource—or if the entity context prohibits state transformation based on backend business rules—the API silently curtails providing those precise modifier links, centralizing business rule enforcement and vastly streamlining complex client-side UI evaluations.

### Q3 (Part 2.1): What are the trade-offs of returning only IDs versus full room objects in a list response? Consider bandwidth and client-side processing.
When an API retrieves a collection of resources, engineering a protocol that returns strictly resource identifiers instead of transmitting fully serialized nested objects introduces profound architectural trade-offs affecting both bandwidth constraints and operational processing latencies. From a bandwidth perspective, transmitting a strict array of basic textual IDs radically reduces the overall Byte payload magnitude transported via the TCP layer, thereby conserving intrinsic server outbound capabilities and accelerating transmission throughput for significantly heavy transaction environments. However, this minimalist approach frequently materializes the notorious N+1 computational problem on the client side. Generating UI elements or executing localized filtering requires the consuming interface to trigger individual subsequent asynchronous HTTP GET operations for each target ID, introducing immense network communication latency overhead while generating unnecessary connection management processing constraints inside the executing browser. Alternatively, dispatching entirely structured objects resolves this inherent penalty by loading all imperative domain variables via a singular, highly efficient transactional request. While this heavily minimizes cross-network execution delays, it can disastrously congest local network interfaces if the backend returns thousands of entities containing profound polymorphic layers, triggering substantial CPU exhaustion during rapid JSON string deserialization steps executed upon constrained endpoint devices.

### Q4 (Part 2.2): Is DELETE idempotent in this implementation? What happens if the same DELETE request is sent multiple times for the same room?
The architectural definition of idempotency dictates that executing a single identical transactional operation repeatedly must consistently yield identical absolute server state conditions, entirely without compounding adverse systemic effects. In standard proper HTTP protocol implementations, the DELETE operation rigorously fulfills this fundamental requirement. When an authentic client dispatches the initial DELETE query targeting a discrete, active room entity interface, the database storage execution tier permanently purges the linked record object, correctly providing an HTTP 204 No Content output status signifying successful operational execution without an accompanying response payload. If the identical client or an overlapping consumer processes identically fabricated duplicate DELETE requests targeting that precise, currently nullified entity instance, the backend execution controller correctly determines that the localized resource strictly does completely lack physical presence. Under this circumstance, the server rejects processing the instruction while successfully emitting an HTTP 404 Not Found outcome. Despite the technical discontinuity between the 204 and 404 status codes produced computationally throughout subsequent interactions, the overarching domain storage application condition specifically validates absolute operational idempotency because the specific room intrinsically remains permanently excluded from the active system state inventory.

### Q5 (Part 3.1): What happens technically when a client sends a POST with Content-Type: text/plain when the endpoint declares @Consumes(MediaType.APPLICATION_JSON)?
When a JAX-RS endpoint explicitly configures the `@Consumes(MediaType.APPLICATION_JSON)` annotation, the underlying framework container dynamically establishes rigorous connection routing prerequisite rules strictly based on evaluating the incoming request’s HTTP `Content-Type` specification header. During execution, providing a consumer attempts an explicit POST transmission bearing a `Content-Type: text/plain` signature, the deployed application server intercepts the network payload at its outermost boundary filtering mechanism preceding any standard Java application logic. The routing controller extensively investigates the embedded metadata profile and determines a definitive, unresolvable discrepancy between the requested content classification and registered endpoint allowance protocols. This fundamental mismatch triggers an immediate pre-emptive operational shutdown of the specified interaction pathway. The execution thread abandons progressing toward the specific internal Resource class entirely; consequently, the embedded textual payload successfully bypasses complex deserialization engines such as Jackson. Simultaneously, the framework dynamically instantiates an unalterable HTTP 415 Unsupported Media Type notification to inform the client accurately concerning their fundamentally incorrect request design. This integrated evaluation architecture profoundly insulates core procedural structures by actively preventing internal computational routines from processing unpredictable, improperly formatted, or explicitly malicious plaintext packet variables.

### Q6 (Part 3.2): Why is @QueryParam preferred over embedding the filter value in the URL path (e.g., /sensors/type/CO2) for collection filtering?
Proper modern RESTful structural philosophy emphatically dictates that operational URL path segments serve explicitly to identify precise individual entities or holistic functional collections natively nested inside a distinct relational domain. Injecting search properties natively inside the static route trajectory, such as projecting execution via `/sensors/type/CO2`, catastrophically violates this architectural canon by permanently conflating innate structural identity identifiers with highly volatile computational filtering processes. Transitioning application configurations toward adopting the `@QueryParam` mechanism, effectively producing requests like `/sensors?type=CO2`, ensures absolute preservation identifying exactly the correct foundational resource layer targeting the base `/sensors` compilation structure. Query variables properly separate algorithmic parameter filtering arguments away from physical network routing mapping operations. Additionally, this methodological framework explicitly supports naturally accumulating arbitrarily massive variations comprising distinct multi-parameter filtering queries completely without fundamentally redesigning monolithic URL pattern hierarchies across every minor frontend application update. Such methodologies inherently offer vastly superior flexibility regarding advanced configuration permutations controlling response limits, data pagination sequences, or specific alphabetical sorting constraints while explicitly avoiding bloated routing endpoints.

### Q7 (Part 4.1): What are the architectural benefits of the Sub-Resource Locator pattern? How does it manage complexity vs. defining all nested paths in one controller?
Implementing the sophisticated Sub-Resource Locator paradigm strongly reinforces imperative domain separation concepts via vastly elevated modular cohesion models while thoroughly eliminating centralized monolith class routing bottlenecks. Implementing purely conventional routing systems forces engineering teams strictly into aggressively writing thousands of individual methods universally mapping every discrete downstream `/sensors/{id}/readings/...` hierarchy specifically onto a single congested master endpoint file structure. Conversely, deploying proper Sub-Resource Locator methodologies radically re-configures processing architecture so the primary parent `SensorResource` essentially operates merely as a specialized forwarding distributor upon intercepting matching network requests traversing nested segments. By strictly discarding the `@GET` or `@POST` HTTP verb annotations structurally, this specialized master routing function instantiates and actively returns independent, completely decoupled child classes handling explicitly defined sub-domain processes representing the sensor metric values. The executing application container subsequently automatically redirects downstream HTTP instructions universally toward that dedicated returned component object. This fundamentally manages architectural complexity extensively because individual child controllers distinctly process respective domain operations universally isolated from managing explicit root parameter references universally repeated everywhere. This directly promotes significantly enhanced testing, absolute decoupling, and vastly streamlined functional maintainability.

### Q8 (Part 5.2): Why is HTTP 422 more semantically accurate than 404 when a sensor references a roomId that doesn't exist?
When investigating the correct response status regarding foreign structural reference mappings, returning an HTTP 404 Not Found typically implies that the fundamental network request structurally arrived at a completely non-existent physical internet location or specified an unreachable parent execution root URI structure. Generally, a developer successfully contacts the completely legitimate and active `/sensors` master HTTP domain target applying completely appropriate JSON syntax mappings containing no explicit formatting anomalies. However, whenever that meticulously parsed data dictionary comprises a fundamentally unrecognized constraint variable essentially acting as a non-existent relational database `roomId` foreign key matching instruction, the primary interaction request format exists perfectly. Selecting an HTTP 422 Unprocessable Entity accurately projects critical semantic realism mapping into API protocol exchanges because it directly signifies that while the server distinctly interprets request formatting requirements successfully against documented MIME validation algorithms, executing embedded business methodologies inherently fails. The domain system explicitly halts execution entirely because validating the requested functional instructions structurally yields logic failure paths completely contradicting underlying database constraints. Using a 422 profoundly communicates internal entity data inconsistency issues, vastly superior to suggesting entirely disconnected transmission endpoint failures.

### Q9 (Part 5.4): From a cybersecurity standpoint, what specific risks arise from exposing raw Java stack traces to external API consumers?
Displaying unfiltered internal engine stack tracing exceptions directly toward unauthenticated external client networks critically generates extremely pronounced catastrophic cybersecurity vulnerabilities resulting strictly from involuntary infrastructure intelligence leaking mechanisms. Outputting unmanaged underlying Java compilation errors immediately divulges substantial hidden framework design compositions, illuminating extremely precise deployment class path hierarchies identifying custom foundational architecture execution parameters globally. Furthermore, these profound errors constantly publicize granular specifications detailing highly specialized external integration libraries, identifying implicit runtime engine implementations and explicit component build versions controlling Jackson processing sequences, Jersey structural systems, or Hibernate entity interactions. Aggressively accumulating this highly technical data layer completely eliminates essential operational enumeration hurdles universally burdening automated penetration adversaries fundamentally searching systems toward identifying exploitable vectors. The structural data significantly accelerates an adversary’s extensive initial network mapping configuration operation, rapidly permitting precise tactical alignments connecting strictly acknowledged system dependency implementations completely mirroring globally accessible Common Vulnerabilities and Exposures datasets. Ultimately, exploiting the extracted technical error parameters strongly authorizes executing intensely curated runtime attacks exploiting exact vulnerable methods generating pathways towards achieving massive database extraction or achieving comprehensive remote machine exploitation functionality.

### Q10 (Part 5.5): Why is using JAX-RS filters for logging superior to manually inserting Logger.info() calls inside every resource method?
Structurally adopting standardized foundational JAX-RS `ContainerRequestFilter` alongside synchronized `ContainerResponseFilter` interfaces explicitly establishes strict Cross-Cutting Concern architectural patterns meticulously decoupling standard environmental telemetry infrastructure processing from highly optimized application business domain variables via systematic aspect-oriented interception layers. Conversely, forcefully executing manual framework transcription inserting basic `Logger.info()` invocation statements systematically duplicated across comprehensively diverse class execution operations profoundly establishes extreme explicit coding mechanism coupling paradigms fundamentally destroying application portability models. This extensively violates underlying Single Responsibility Principles governing object-oriented architectures fundamentally guaranteeing uncontrolled massive codebase source duplication operations. Transitioning directly into deploying explicit component filters strategically centralizes comprehensive execution observability mechanics capturing extremely fundamental API operational constants comprehensively encompassing foundational HTTP execution variables, distinct uniform routing network paths, comprehensive processing runtime execution chronometrics, and overarching data payload dimension assessments structurally via solitary universal proxy interfaces universally intercepting every individual execution thread proceeding prior or immediately concluding standard application routing operations. This thoroughly establishes comprehensive, immutable infrastructural auditability constraints universally applying tracking metrics flawlessly guaranteeing complete component execution evaluation without universally cluttering imperative resource class functional coding.

## 8. Known Limitations
- **Volatile Execution Persistence**: The application incorporates `ConcurrentHashMap` objects implemented purely via RAM parameters representing temporary mapping states, assuring universally total data destruction resulting from standard execution environment termination commands or container interruptions.
- **Disconnected Access Configuration**: Internal HTTP methodologies categorically lack intrinsic OAuth2 or programmatic mutual TLS security layers controlling packet ingress execution pathways, mandating isolated deployments avoiding open external gateway routing completely.
- **Architectural Concurrency Inefficiencies**: Specific parent object modifications traversing dependent nested items (exemplified via checking cascaded bindings traversing DELETE Room structures mapped dynamically toward active target sensors) necessity executing compound data structural evaluations possessing zero underlying database isolation transaction configurations.
- **Absolute Runtime Heap Accumulation**: Continuous generation capturing telemetric device inputs generates profoundly persistent temporal reading object instantiations mapping strictly onto operational execution boundaries employing zero pruning configuration techniques, triggering fundamentally inevitable terminal server out-of-memory exception terminations under indefinite application testing loads.
