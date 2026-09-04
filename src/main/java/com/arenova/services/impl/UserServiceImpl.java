package com.arenova.services.impl;

import com.arenova.dtos.ChangePasswordRequest;
import com.arenova.dtos.PreferredGamesRequest;
import com.arenova.dtos.UpdateStatusRequest;
import com.arenova.dtos.UserDTO;
import com.arenova.dtos.enums.Role;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.UserMapper;
import com.arenova.respositories.UserRepository;
import com.arenova.security.PasswordPolicy;
import com.arenova.services.UserService;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @Override
    public UserDTO createUser(UserDTO userDTO) throws BadRequestException {
        User user = UserMapper.toEntity(userDTO); // changing data to entity to save in repo

        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            PasswordPolicy.requireStrongPassword(user.getPassword());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        User savedUser =  userRepository.save(user); //saving in repo + exracting data and saving to saved user
        return UserMapper.toDTO(savedUser); //return data to frontend
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("User ID Not Found")); //Entity value
        return UserMapper.toDTO(user);
    }

    @Override
    public void updateProfilePage(Long id, String photoUrl) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User Not Found"));
        user.setProfilePhotoUrl(photoUrl);
        userRepository.save(user);
    }

    @Override
    public void updateProfilePhoto(Long id, String photoUrl) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User Not Found"));
        user.setProfilePhotoUrl(photoUrl);
        userRepository.save(user);
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) throws BadRequestException {
        User user =  userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User ID NOT FOUND"));

        if (userDTO.getUsername() != null) {
            user.setUsername(userDTO.getUsername());
        }
        if (userDTO.getFullName() != null) {
            user.setFullName(userDTO.getFullName());
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getContact() != null) {
            user.setContact(userDTO.getContact());
        }
        if (userDTO.getBio() != null) {
            user.setBio(userDTO.getBio());
        }
        if (userDTO.getPreferredGames() != null) {
            user.setPreferredGames(userDTO.getPreferredGames());
        }
        // Only change password when a new one is sent (do not wipe existing hash)
        if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
            PasswordPolicy.requireStrongPassword(userDTO.getPassword());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

       User updatedUser = userRepository.save(user);
       return UserMapper.toDTO(updatedUser);
    }

    @Override
    public void deleteUser(long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("USER ID NOT FOUND"));
        userRepository.delete(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
       List<User> users = userRepository.findAll();
        return users.stream().map(UserMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String changePassword(Long id, ChangePasswordRequest request) throws BadRequestException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User ID NOT FOUND"));

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new BadRequestException("Current password is required.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new BadRequestException("New password is required.");
        }
        PasswordPolicy.requireStrongPassword(request.getNewPassword());
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password updated successfully.";
    }

    @Override
    public UserDTO updateStatus(Long id, UpdateStatusRequest request) throws BadRequestException {
        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User ID NOT FOUND"));

        user.setStatus(request.getStatus());
        User saved = userRepository.save(user);
        return UserMapper.toDTO(saved);
    }

    @Override
    public UserDTO updatePreferredGames(Long id, PreferredGamesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User ID NOT FOUND"));

        if (request.getPreferredGames() == null || request.getPreferredGames().isEmpty()) {
            user.setPreferredGames(null);
        } else {
            user.setPreferredGames(
                    request.getPreferredGames()
                            .stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.joining(","))
            );
        }

        User saved = userRepository.save(user);
        return UserMapper.toDTO(saved);
    }

}
