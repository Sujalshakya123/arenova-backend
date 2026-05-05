package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class UserDTO {

    private Long id;
    private String username;
    private String contact;
    private String email;
    private String password;

}
