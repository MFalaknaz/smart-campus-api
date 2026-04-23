package com.westminster.smartcampus.config;

import com.westminster.smartcampus.filter.ApiLoggingFilter;
import com.westminster.smartcampus.mapper.GlobalExceptionMapper;
import com.westminster.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.westminster.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.westminster.smartcampus.resource.DiscoveryResource;
import com.westminster.smartcampus.resource.RoomResource;
import com.westminster.smartcampus.resource.SensorResource;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS application entry point.
 * All resources, providers, filters, and features are registered here explicitly.
 */
@ApplicationPath("/api/v1")
public class AppConfig extends ResourceConfig {

    public AppConfig() {
        // Resources
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);

        // Exception mappers
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalExceptionMapper.class);

        // Filters
        register(ApiLoggingFilter.class);

        // JSON support via Jackson
        register(JacksonFeature.class);
    }
}
