package com.arenova.mapper;

import com.arenova.dtos.UserDTO;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.User;

public class UserMapper {

        //method User Entity - user DTO convert
        // password is not returned to frontend (security)
        public static UserDTO toDTO (User user){
            return new UserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getContact(),
                    user.getEmail(),
                    null,
                    user.getRole(),
                    user.getAuthProvider(),
                    user.getProfilePhotoUrl(),
                    user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE,
                    user.getBio(),
                    user.getPreferredGames()
            );
        }

        //User DTO - User Entity Convert
        public static User toEntity(UserDTO userDTO){
            return new User(
                    userDTO.getId(),
                    userDTO.getUsername(),
                    userDTO.getFullName(),
                    userDTO.getContact(),
                    userDTO.getEmail(),
                    userDTO.getPassword(),
                    userDTO.getRole(),
                    userDTO.getAuthProvider(),
                    userDTO.getProfilePhotoUrl(),
                    userDTO.getStatus() != null ? userDTO.getStatus() : UserStatus.ACTIVE,
                    userDTO.getBio(),
                    userDTO.getPreferredGames(),
                    null
            );
        }
}
