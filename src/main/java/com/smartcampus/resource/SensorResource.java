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
import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    // GET /api/v1/sensors
    @GET
    public Collection<Sensor> getAllSensors(@QueryParam("type") String type) {

        if (type == null) {
            return DataStore.sensors.values();
        }

        return DataStore.sensors.values()
                .stream()
                .filter(sensor -> sensor.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    // POST /api/v1/sensors
    @POST

    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {

        // 1. GENERATE THE ID IF MISSING (The Fix)
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            sensor.setId(java.util.UUID.randomUUID().toString());
        }

        // 2. Validate Room ID is provided
        if (sensor.getRoomId() == null || sensor.getRoomId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Room ID is required."))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // 3. Validate Room actually exists
        Room room = DataStore.rooms.get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room with ID '" + sensor.getRoomId() + "' does not exist.");
        }

        // 4. Save and link the sensor
        DataStore.sensors.put(sensor.getId(), sensor);
        room.getSensorIds().add(sensor.getId());

        // 5. Return 201 Created with just the ID
        return Response.status(Response.Status.CREATED)
                .entity(java.util.Collections.singletonMap("id", sensor.getId()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(
            @PathParam("sensorId") String sensorId) {

        return new SensorReadingResource(sensorId);
    }
}