package com.westminster.smartcampus.dto;

/**
 * Uniform error payload returned by all exception mappers and resource-level error responses.
 */
public class ErrorResponse {

    private String error;
    private String message;
    private int status;
    private long timestamp;

    public ErrorResponse() {}

    public ErrorResponse(String error, String message, int status) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
