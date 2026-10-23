package com.aviral.transact.service;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id) { super("Account " + id + " was not found"); }
}
