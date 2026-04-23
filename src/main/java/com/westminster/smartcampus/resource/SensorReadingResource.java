package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.dto.CreateReadingRequest;
import com.westminster.smartcampus.dto.ErrorResponse;
import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;
import com.westminster.smartcampus.store.InMemoryStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sub-resource for sensor readings.
 * Accessed via: /api/v1/sensors/{sensorId}/readings
 * Instantiated by the sub-resource locator in SensorResource — not registered directly.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final InMemoryStore store = InMemoryStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return notFound("Sensor with id '" + sensorId + "' was not found.");
        }
        List<SensorReading> readings = store.getReadingsBySensorId()
                .getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(CreateReadingRequest request, @Context UriInfo uriInfo) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return notFound("Sensor with id '" + sensorId + "' was not found.");
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }
        if (request == null) {
            return badRequest("Reading payload with 'value' field is required.");
        }

        SensorReading reading = new SensorReading(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                request.getValue()
        );

        store.getReadingsBySensorId()
                .computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>())
                .add(reading);

        sensor.setCurrentValue(reading.getValue());

        URI location = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(location).entity(reading).build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Bad Request", message, 400))
                .build();
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Not Found", message, 404))
                .build();
    }
}
