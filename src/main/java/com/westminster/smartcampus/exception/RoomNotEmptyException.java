package com.westminster.smartcampus.exception;

/**
 * Thrown when a DELETE request targets a room that still has sensors assigned.
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;
    private final int sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room " + roomId + " cannot be deleted because it still has "
                + sensorCount + " assigned sensor(s). Reassign or remove all sensors first.");
        this.roomId = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId() { return roomId; }
    public int getSensorCount() { return sensorCount; }
}
