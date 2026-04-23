package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.dto.CreateSensorRequest;
import com.westminster.smartcampus.dto.ErrorResponse;
import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.store.InMemoryStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final InMemoryStore store = InMemoryStore.getInstance();

    /**
     * Returns all sensors, optionally filtered by type query parameter.
     * Example: GET /api/v1/sensors?type=CO2
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(store.getSensors().values());
        if (type != null && !type.isBlank()) {
            list = list.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(list).build();
    }

    @POST
    public Response createSensor(CreateSensorRequest request, @Context UriInfo uriInfo) {
        if (request == null || isBlank(request.getId())) {
            return badRequest("Sensor 'id' is required.");
        }
        if (isBlank(request.getType())) {
            return badRequest("Sensor 'type' is required.");
        }
        if (isBlank(request.getRoomId())) {
            return badRequest("Sensor 'roomId' is required.");
        }
        if (!store.getRooms().containsKey(request.getRoomId())) {
            throw new LinkedResourceNotFoundException(request.getRoomId());
        }
        if (store.getSensors().containsKey(request.getId())) {
            return conflict("A sensor with id '" + request.getId() + "' already exists.");
        }

        String status = isBlank(request.getStatus()) ? "ACTIVE" : request.getStatus().toUpperCase();

        Sensor sensor = new Sensor(
                request.getId(),
                request.getType(),
                status,
                request.getCurrentValue(),
                request.getRoomId()
        );

        store.getSensors().put(sensor.getId(), sensor);
        store.getRooms().get(request.getRoomId()).getSensorIds().add(sensor.getId());
        store.getReadingsBySensorId().put(sensor.getId(), new CopyOnWriteArrayList<>());

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return notFound("Sensor with id '" + sensorId + "' was not found.");
        }
        return Response.ok(sensor).build();
    }

    /**
     * Sub-resource locator — delegates /sensors/{sensorId}/readings to SensorReadingResource.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

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

    private Response conflict(String message) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("Conflict", message, 409))
                .build();
    }
}
