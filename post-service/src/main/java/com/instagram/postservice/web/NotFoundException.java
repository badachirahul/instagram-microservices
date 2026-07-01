package com.instagram.postservice.web;

/** Thrown when a referenced post does not exist — mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
