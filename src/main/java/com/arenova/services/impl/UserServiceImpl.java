package com.arenova.services.impl;

import com.arenova.dtos.UserDTO;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.UserMapper;
import com.arenova.respositories.UserRepository;
import com.arenova.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;


    @Override
    public UserDTO createUser(UserDTO userDTO) {
        User user = UserMapper.toEntity(userDTO); // changing data to entity to save in repo
        User savedUser =  userRepository.save(user); //saving in repo + exracting data and saving to saved user
        return UserMapper.toDTO(savedUser); //return data to frontend
    }


    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("User ID Not Found")); //Entity value
        return UserMapper.toDTO(user);
    }

//    @Override
//    public UserDTO getUserByUsername(String username) {
//        User user  = userRepository.findByUsername(username).orElseThrow(()->new ResourceNotFoundException("username not found"));
//        return UserMapper.toDTO(user);
//    }
//
//    @Override
//    public UserDTO updateUser(UserDTO userDTO, int id) {
//        User user  = userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User with given id not found" + id));
//        user.setUsername(userDTO.getUsername());
//        user.setEmail(userDTO.getEmail());
//        user.setPassword(userDTO.getPassword());
//        user.setContact(userDTO.getContact());
//
//
//        User updatedUser = userRepository.save(user);
//        return UserMapper.toDTO(updatedUser);
//    }
//
//    @Override
//    public void deleteUser(int id) {
//        User user  = userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User id not found"));
//        userRepository.delete(user);  //void vayeko le return hudaina
//    }
//
//    @Override
//    public List<UserDTO> getAllUser() {
//        List<User> users = userRepository.findAll();
//        return users.stream().map((user) -> UserMapper.toDTO(user)).collect(Collectors.toList());
//    }


}
