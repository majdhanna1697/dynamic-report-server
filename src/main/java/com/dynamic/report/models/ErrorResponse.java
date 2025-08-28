package com.dynamic.report.models;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ErrorResponse {

    private String errorCode;
    private String errorMessage;
    private String requestURI;
    private Map<String, String> validationErrors = new HashMap<>();

}
