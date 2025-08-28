package com.dynamic.report.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class TokenException extends Exception {

    private int code;
    private HttpStatus status;

    public TokenException(int code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

}
