package com.kaung.uit_virtual_campus.exception;

public class InvalidJwtSubjectException extends RuntimeException {

    public InvalidJwtSubjectException(String subject) {
        super("JWT subject is not a valid UUID: " + subject);
    }
}
