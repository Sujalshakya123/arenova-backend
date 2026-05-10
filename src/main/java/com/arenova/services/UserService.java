package com.arenova.services;

import com.arenova.dtos.UserDTO;

import java.util.List;

public interface UserService {
    //Create User
    UserDTO createUser(UserDTO userDTO);

    //Update User
    UserDTO updateUser(Long id, UserDTO userDTO);

    //Delete Uer
    void deleteUser(long id);

    //Get all users
    List<UserDTO> getAllUsers();


    //Get user by id
    UserDTO getUserById(Long id);


    //Get user by Username
//    UserDTO getUserByUsername(String username);
}
