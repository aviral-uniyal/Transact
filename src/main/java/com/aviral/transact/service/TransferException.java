package com.aviral.transact.service;

import org.springframework.http.HttpStatus;

public class TransferException extends RuntimeException {
    private final HttpStatus status;

    public TransferException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
