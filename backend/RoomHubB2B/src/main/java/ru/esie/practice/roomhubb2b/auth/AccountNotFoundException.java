package ru.esie.practice.roomhubb2b.auth;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Authenticated account is no longer available");
    }
}
