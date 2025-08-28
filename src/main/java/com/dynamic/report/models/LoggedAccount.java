package com.dynamic.report.models;

import lombok.Data;

import java.math.BigInteger;

@Data
public class LoggedAccount {

    private BigInteger id;
    private String password;

}
