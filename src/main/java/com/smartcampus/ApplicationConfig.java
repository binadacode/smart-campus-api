package com.smartcampus;

import org.glassfish.jersey.server.ResourceConfig;
import jakarta.ws.rs.ApplicationPath;
import com.smartcampus.exception.mapper.GlobalExceptionMapper;
import com.smartcampus.exception.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.filter.ApiLoggingFilter;

/**
 * JAX-RS configuration class for the Smart Campus API.
 * This replaces the programmatic Grizzly server configuration.
 */
@ApplicationPath("/api/v1")
public class ApplicationConfig extends ResourceConfig {

    public ApplicationConfig() {
        // Scan for resources in the specified package
        packages("com.smartcampus.resource");

        // Register custom Exception Mappers
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(RoomNotEmptyExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalExceptionMapper.class);

        // Register Filters
        register(ApiLoggingFilter.class);
        
        // Output confirmation to console on startup
        System.out.println("Smart Campus API Initialized at /api/v1");
    }
}
