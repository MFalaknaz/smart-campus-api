package com.westminster.smartcampus.exception;

/**
 * Thrown when a POST /sensors request references a roomId that does not exist.
 * The request JSON is structurally valid, but the linked resource is absent.
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String referencedId;

    public LinkedResourceNotFoundException(String referencedId) {
        super("The referenced room with id '" + referencedId
                + "' does not exist. The request is well-formed but references an unknown resource.");
        this.referencedId = referencedId;
    }

    public String getReferencedId() { return referencedId; }
}
