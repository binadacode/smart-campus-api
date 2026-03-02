package com.smartcampus.resource;

import com.smartcampus.DataStore;
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
@Consumes(MediaType.APPLICATION_JSON)
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
    public Response createSensor(Sensor sensor) {

        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sensor ID is required.")
                    .build();
        }

        if (sensor.getRoomId() == null || sensor.getRoomId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Room ID is required.")
                    .build();
        }

        Room room = DataStore.rooms.get(sensor.getRoomId());

        if (room == null) {
            return Response.status(422)
                    .entity("Referenced room does not exist.")
                    .build();
        }

        DataStore.sensors.put(sensor.getId(), sensor);

        // Link sensor to room
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED)
                .entity(sensor)
                .build();
    }
}