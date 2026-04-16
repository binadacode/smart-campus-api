package com.smartcampus.exception.mapper;

import com.smartcampus.exception.SensorUnavailableException;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {

        String json = "{\"error\":\"" + ex.getMessage() + "\"}";

        return Response.status(Response.Status.FORBIDDEN)
                .entity(json)
                .type("application/json")
                .build();
    }
}