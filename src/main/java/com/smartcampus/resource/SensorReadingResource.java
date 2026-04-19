package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ErrorResponse;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        if (!DataStore.sensors.containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Sensor not found."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        List<SensorReading> readings = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());

        return Response.ok(readings).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Sensor not found."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is under maintenance and cannot accept new readings.");
        }

        // Server is authoritative for both ID and timestamp generation.
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(Instant.now().toString()); // ISO-8601, e.g. "2024-04-18T10:00:00Z"

        DataStore.readings
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        // reading.getValue() returns a primitive double — box it so we can null-check
        // before passing to sensor.setCurrentValue(Double). Prevents unboxing NPE
        // if the request body omits the "value" field entirely.
        Double readingValue = reading.getValue();
        if (readingValue != null) {
            sensor.setCurrentValue(readingValue);
        }

        return Response.status(Response.Status.CREATED)
                .entity(Collections.singletonMap("id", reading.getId()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}