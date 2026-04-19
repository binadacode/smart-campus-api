package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    // GET /api/v1/rooms
    @GET
    public Collection<Room> getAllRooms() {
        return DataStore.rooms.values();
    }

    // POST /api/v1/rooms
    // POST /api/v1/rooms
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {

        // 1. Generate the ID on the server (Delete the old null check)
        String generatedId = java.util.UUID.randomUUID().toString();
        room.setId(generatedId);

        // 2. Save to DataStore
        DataStore.rooms.put(generatedId, room);

        // 3. Use Map.of() for safe JSON serialization
        return Response.status(Response.Status.CREATED)
                .entity(java.util.Map.of("id", generatedId))
                .build();
    }

    // GET /api/v1/rooms/{id}
    @GET
    @Path("/{id}")
    public Response getRoomById(@PathParam("id") String id) {

        Room room = DataStore.rooms.get(id);

        if (room == null) {
            // Return error as a structured JSON object
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(java.util.Map.of("error", "Room not found."))
                    .build();
        }

        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{id}
    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") String id) {

        Room room = DataStore.rooms.get(id);

        if (room == null) {
            // Return error as a structured JSON object
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(java.util.Map.of("error", "Room not found."))
                    .build();
        }

        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room contains active sensors.");
        }

        DataStore.rooms.remove(id);

        return Response.noContent().build();
    }
}