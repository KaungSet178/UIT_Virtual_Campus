package com.kaung.uit_virtual_campus.exception;

import java.util.UUID;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException(UUID userId) {
        super("Authenticated application user was not found: " + userId);
    }
}
