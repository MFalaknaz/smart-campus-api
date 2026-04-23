package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.dto.CreateRoomRequest;
import com.westminster.smartcampus.dto.ErrorResponse;
import com.westminster.smartcampus.exception.RoomNotEmptyException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.store.InMemoryStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final InMemoryStore store = InMemoryStore.getInstance();

    @GET
    public Response getAllRooms() {
        List<Room> list = new ArrayList<>(store.getRooms().values());
        return Response.ok(list).build();
    }

    @POST
    public Response createRoom(CreateRoomRequest request, @Context UriInfo uriInfo) {
        if (request == null || isBlank(request.getId())) {
            return badRequest("Room 'id' is required.");
        }
        if (isBlank(request.getName())) {
            return badRequest("Room 'name' is required.");
        }
        if (request.getCapacity() <= 0) {
            return badRequest("Room 'capacity' must be a positive integer.");
        }
        if (store.getRooms().containsKey(request.getId())) {
            return conflict("A room with id '" + request.getId() + "' already exists.");
        }

        Room room = new Room(request.getId(), request.getName(), request.getCapacity());
        store.getRooms().put(room.getId(), room);

        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return notFound("Room with id '" + roomId + "' was not found.");
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return notFound("Room with id '" + roomId + "' was not found.");
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build();
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
