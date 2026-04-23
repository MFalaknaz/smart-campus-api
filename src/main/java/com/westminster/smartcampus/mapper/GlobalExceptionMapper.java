package com.westminster.smartcampus.mapper;

import com.westminster.smartcampus.dto.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all exception mapper.
 * Intercepts any Throwable not handled by a more specific mapper and returns
 * a safe 500 response that does not expose internal implementation details.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        LOGGER.log(Level.SEVERE, "Unhandled exception intercepted by GlobalExceptionMapper", ex);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(
                        "Internal Server Error",
                        "An unexpected error occurred. Please contact the system administrator.",
                        500
                ))
                .build();
    }
}
