package com.arenova.controllers;

import com.arenova.dtos.UserDTO;
import com.arenova.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;


//http ma kaam garxa
//controller is depended on user service layer
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO){
        UserDTO savedUser = userService.createUser(userDTO);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }


    @GetMapping("{id}")
    public ResponseEntity<UserDTO>  getUserById(@PathVariable() Long id){
        UserDTO user = userService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.FOUND);
    }

    @PutMapping("{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO){
        UserDTO user = userService.updateUser(id, userDTO);
        return new ResponseEntity<>(user , HttpStatus.OK);
    }


    @DeleteMapping("{id}")
    public ResponseEntity<UserDTO> deleteUser(@PathVariable long id){
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(){
        List<UserDTO> users = userService.getAllUsers();
        return new ResponseEntity<>(users , HttpStatus.OK);
    }

    //Photo
    @PostMapping("/{id}/upload-photo")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile file
    ) {
        try {
            // Create uploads folder if it doesn't exist
            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            // Generate unique filename
            String filename = UUID.randomUUID() + ".jpg";
            Path filePath = Paths.get(uploadDir + filename);

            // Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Build the URL
            String photoUrl = "http://localhost:8080/uploads/" + filename;

            // Save URL to DB via service
            userService.updateProfilePhoto(id, photoUrl);

            return ResponseEntity.ok(Map.of("photoUrl", photoUrl));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}

