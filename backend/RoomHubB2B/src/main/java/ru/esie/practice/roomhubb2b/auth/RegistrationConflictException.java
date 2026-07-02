package ru.esie.practice.roomhubb2b.auth;

public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException() {
        super("An account with this email or tax ID is already registered");
    }
}
