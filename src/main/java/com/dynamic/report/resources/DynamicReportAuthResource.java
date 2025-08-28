package com.dynamic.report.resources;

import com.asia.api.AuthenticationApi;
import com.asia.model.LoginRequest;
import com.asia.model.LoginResponse;
import com.dynamic.report.controllers.DynamicReportAuthController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class DynamicReportAuthResource implements AuthenticationApi {

    @Autowired
    private DynamicReportAuthController dynamicReportAuthController;

    @Override
    public ResponseEntity<LoginResponse> login(String authorization, LoginRequest loginRequest) throws Exception {
        return new ResponseEntity<>(dynamicReportAuthController.login(authorization, loginRequest), HttpStatus.OK);
    }
}
