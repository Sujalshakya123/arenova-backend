package com.arenova.controllers;

import com.arenova.dtos.AdminOrganizerDTO;
import com.arenova.dtos.AdminPlayerDTO;
import com.arenova.dtos.UpdateStatusRequest;
import com.arenova.dtos.UserDTO;
import com.arenova.services.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/organizers")
    public ResponseEntity<List<AdminOrganizerDTO>> listOrganizers() {
        return ResponseEntity.ok(adminUserService.listOrganizers());
    }

    @GetMapping("/players")
    public ResponseEntity<List<AdminPlayerDTO>> listPlayers() {
        return ResponseEntity.ok(adminUserService.listPlayers());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request
    ) throws BadRequestException {
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) throws BadRequestException {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
