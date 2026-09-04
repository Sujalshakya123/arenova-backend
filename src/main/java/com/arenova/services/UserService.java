package com.arenova.services;

import com.arenova.dtos.ChangePasswordRequest;
import com.arenova.dtos.PreferredGamesRequest;
import com.arenova.dtos.UpdateStatusRequest;
import com.arenova.dtos.UserDTO;
import com.arenova.dtos.enums.Role;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface UserService {
    //Create User
    UserDTO createUser(UserDTO userDTO) throws BadRequestException;

    //Update User
    UserDTO updateUser(Long id, UserDTO userDTO) throws BadRequestException;

    //Delete Uer
    void deleteUser(long id);

    //Get all users
    List<UserDTO> getAllUsers();

    //Get users by role (PLAYER / ORGANIZER / ADMIN)
    List<UserDTO> getUsersByRole(Role role);


    //Get user by id
    UserDTO getUserById(Long id);


    void updateProfilePage(Long id, String photoUrl);

    void updateProfilePhoto(Long id, String photoUrl);

    String changePassword(Long id, ChangePasswordRequest request) throws BadRequestException;

    UserDTO updateStatus(Long id, UpdateStatusRequest request) throws BadRequestException;

    UserDTO updatePreferredGames(Long id, PreferredGamesRequest request);

    //Get user by Username
//    UserDTO getUserByUsername(String username);
}
