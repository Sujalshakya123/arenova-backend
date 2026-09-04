package com.arenova.dtos;

import lombok.Data;

import java.util.List;

@Data
public class RegisterEventRequest {

    private String teamName;
    private String teamTag;
    private String captainUsername;
    private List<String> roster;
    private String paymentMethod;
}
