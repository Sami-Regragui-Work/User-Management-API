package org.spring.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
    public InvalidPasswordException() {
        this("Current password is incorrect");
    }
}
