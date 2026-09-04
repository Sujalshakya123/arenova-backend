package com.arenova.services;

import com.arenova.dtos.AdminOrganizerDTO;
import com.arenova.dtos.AdminPlayerDTO;
import com.arenova.dtos.UpdateStatusRequest;
import com.arenova.dtos.UserDTO;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface AdminUserService {

    List<AdminOrganizerDTO> listOrganizers();

    List<AdminPlayerDTO> listPlayers();

    UserDTO updateUserStatus(Long id, UpdateStatusRequest request) throws BadRequestException;

    void deleteUser(Long id) throws BadRequestException;
}
