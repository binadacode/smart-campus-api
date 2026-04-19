package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.ErrorResponse;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.Room;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public Collection<Sensor> getAllSensors(@QueryParam("type") String type) {
        if (type == null) {
            return DataStore.sensors.values();
        }

        // Call equalsIgnoreCase on the non-null query param so that a sensor
        // with a null type field never throws a NullPointerException.
        return DataStore.sensors.values()
                .stream()
                .filter(sensor -> type.equalsIgnoreCase(sensor.getType()))
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {

        // Server is authoritative for ID generation.
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        if (sensor.getRoomId() == null || sensor.getRoomId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Room ID is required."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Validates the referenced room exists before persisting the sensor.
        Room room = DataStore.rooms.get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Room with ID '" + sensor.getRoomId() + "' does not exist.");
        }

        DataStore.sensors.put(sensor.getId(), sensor);
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED)
                .entity(Collections.singletonMap("id", sensor.getId()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    // Sub-resource locator — no HTTP verb annotation; JAX-RS delegates routing
    // to SensorReadingResource based on the remainder of the path.
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(
            @PathParam("sensorId") String sensorId) {

        return new SensorReadingResource(sensorId);
    }
}