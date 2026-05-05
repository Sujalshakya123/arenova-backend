package com.arenova.mapper;

import com.arenova.dtos.UserDTO;
import com.arenova.entities.User;

public class UserMapper {

        //method User Entity - user DTO convert
        public static UserDTO toDTO (User user){
            return new UserDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getContact(),
                    user.getUsername(),
                    user.getPassword()
            );
        }

        //User DTO - User Entity Convert
        public static User toEntity(UserDTO userDTO){
            return new User(
                    userDTO.getId(),
                    userDTO.getEmail(),
                    userDTO.getUsername(),
                    userDTO.getContact(),
                    userDTO.getPassword()
            );
        }
}
