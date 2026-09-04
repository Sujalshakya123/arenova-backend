package com.arenova.services.impl;

import com.arenova.dtos.AdminOrganizerDTO;
import com.arenova.dtos.AdminPlayerDTO;
import com.arenova.dtos.UpdateStatusRequest;
import com.arenova.dtos.UserDTO;
import com.arenova.dtos.enums.Role;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.UserMapper;
import com.arenova.respositories.EventRegistrationRepository;
import com.arenova.respositories.EventRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.services.AdminAccessService;
import com.arenova.services.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminAccessService adminAccessService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrganizerDTO> listOrganizers() {
        adminAccessService.requireAdmin();
        return userRepository.findByRole(Role.ORGANIZER).stream()
                .map(this::toOrganizerDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPlayerDTO> listPlayers() {
        adminAccessService.requireAdmin();
        return userRepository.findByRole(Role.PLAYER).stream()
                .map(this::toPlayerDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO updateUserStatus(Long id, UpdateStatusRequest request)
            throws BadRequestException {
        adminAccessService.requireAdmin();
        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot change status of an admin account.");
        }

        user.setStatus(request.getStatus());
        return UserMapper.toDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) throws BadRequestException {
        adminAccessService.requireAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot delete an admin account.");
        }
        userRepository.delete(user);
    }

    private AdminOrganizerDTO toOrganizerDto(User user) {
        long tournaments = eventRepository.countByProject_Organizer_Id(user.getId());
        return AdminOrganizerDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE)
                .tournamentCount(tournaments)
                .registeredAt(user.getCreatedAt())
                .build();
    }

    private AdminPlayerDTO toPlayerDto(User user) {
        long joined = registrationRepository.countByUser_Id(user.getId());
        return AdminPlayerDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE)
                .tournamentsJoined(joined)
                .build();
    }
}
