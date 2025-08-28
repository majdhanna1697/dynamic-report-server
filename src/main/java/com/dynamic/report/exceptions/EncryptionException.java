package com.dynamic.report.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class EncryptionException extends Exception {

    private int code;
    private HttpStatus status;

    public EncryptionException(int code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

}
