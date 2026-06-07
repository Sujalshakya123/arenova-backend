package com.arenova.dtos;

import com.arenova.dtos.enums.AuthProvider;
import com.arenova.dtos.enums.Role;
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
    private Role role;
    private AuthProvider authProvider;

}
