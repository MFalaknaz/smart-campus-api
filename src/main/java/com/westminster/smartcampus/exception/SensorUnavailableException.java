package com.westminster.smartcampus.exception;

/**
 * Thrown when a POST /sensors/{id}/readings targets a sensor whose status is MAINTENANCE.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;

    public SensorUnavailableException(String sensorId) {
        super("Sensor '" + sensorId
                + "' is currently under MAINTENANCE and cannot accept new readings.");
        this.sensorId = sensorId;
    }

    public String getSensorId() { return sensorId; }
}
