package com.arenova.dtos;


import lombok.Data;

@Data
public class VerifyDTO {
    private String email;
    private String otp;
}
