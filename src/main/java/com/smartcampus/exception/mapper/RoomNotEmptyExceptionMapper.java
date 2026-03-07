package com.smartcampus.exception.mapper;

import com.smartcampus.exception.RoomNotEmptyException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {

        String message = "{\"error\":\"Room Conflict\",\"message\":\"Room contains active sensors.\"}";

        return Response.status(Response.Status.CONFLICT)
                .entity(message)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}