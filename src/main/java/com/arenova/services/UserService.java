package com.arenova.services;

import com.arenova.dtos.UserDTO;

import java.util.List;

public interface UserService {
    //Create User
    UserDTO createUser(UserDTO userDTO);

    //Update User
//    UserDTO updateUser(UserDTO userDTO, int id);

    //Delete Uer
//    void deleteUser(int id);

    //Get all users

//    List<UserDTO> getAllUser();


    //Get user by id
    UserDTO getUserById(Long id);


    //Get user by Username
//    UserDTO getUserByUsername(String username);
}
