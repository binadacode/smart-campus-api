package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ErrorResponse;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
        System.out.println("[DIAGNOSTIC] POST /sensors/" + sensorId + "/readings received: " +
                           (reading != null ? "Value=" + reading.getValue() : "NULL reading"));

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null) {
            System.out.println("[DIAGNOSTIC] Sensor " + sensorId + " NOT FOUND.");
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Sensor not found."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            System.out.println("[DIAGNOSTIC] Sensor " + sensorId + " is in MAINTENANCE.");
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is under maintenance and cannot accept new readings.");
        }

        // Server is authoritative for both ID and timestamp generation.
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(java.time.Instant.now().toString());

        DataStore.readings
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        System.out.println("[DIAGNOSTIC] Reading saved. ID=" + reading.getId());

        // Update the sensor's current value for convenience in the GET /sensors response.
        Double readingValue = reading.getValue();
        if (readingValue != null) {
            System.out.println("[DIAGNOSTIC] Updating sensor " + sensorId + " currentValue to " + readingValue);
            sensor.setCurrentValue(readingValue);
        } else {
            System.out.println("[DIAGNOSTIC] Reading value was NULL, sensor currentValue NOT updated.");
        }

        return Response.status(Response.Status.CREATED)
                .entity(Collections.singletonMap("id", reading.getId()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}