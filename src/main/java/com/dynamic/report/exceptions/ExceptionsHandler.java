package com.dynamic.report.exceptions;

import com.dynamic.report.models.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ExceptionsHandler {

    @Value("${spring.application.name}")
    private String applicationName;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionsHandler.class);

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()));
        response.setErrorMessage(exception.getMessage());
        response.setRequestURI(getRequestUri());
        LOGGER.error("Error occurred: {}", exception.getMessage(), exception);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({AuthException.class})
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(applicationName.toUpperCase() + "-" + exception.getCode());
        response.setErrorMessage(exception.getMessage());
        response.setRequestURI(getRequestUri());
        LOGGER.error("AuthException - Error occurred: {}", exception.getMessage(), exception);
        return new ResponseEntity<>(response, exception.getStatus());
    }

    @ExceptionHandler({TokenException.class})
    public ResponseEntity<ErrorResponse> handleTokenException(TokenException exception) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(applicationName.toUpperCase() + "-" + exception.getCode());
        response.setErrorMessage(exception.getMessage());
        response.setRequestURI(getRequestUri());
        LOGGER.error("AuthException - Error occurred: {}", exception.getMessage(), exception);
        return new ResponseEntity<>(response, exception.getStatus());
    }

    @ExceptionHandler({EncryptionException.class})
    public ResponseEntity<ErrorResponse> handleEncryptionException(EncryptionException exception) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(applicationName.toUpperCase() + "-" + exception.getCode());
        response.setErrorMessage(exception.getMessage());
        response.setRequestURI(getRequestUri());
        LOGGER.error("EncryptionException - Error occurred: {}", exception.getMessage(), exception);
        return new ResponseEntity<>(response, exception.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(applicationName.toUpperCase() + "-" + "VALIDATION-ERROR");
        response.setErrorMessage("Validation failed");
        response.setValidationErrors(errors);
        response.setRequestURI(getRequestUri());

        LOGGER.error("Validation error occurred: {}", errors, ex);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private String getRequestUri() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getRequestURI();
        }
        return "Unknown URI";
    }

}