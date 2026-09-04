package com.arenova.dtos;

import lombok.Data;

@Data
public class CreateProjectRequest {
    private String name;
    private String plan;
}
