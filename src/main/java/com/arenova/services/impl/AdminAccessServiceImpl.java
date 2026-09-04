package com.arenova.services.impl;

import com.arenova.dtos.enums.Role;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.respositories.UserRepository;
import com.arenova.services.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAccessServiceImpl implements AdminAccessService {

    private final UserRepository userRepository;

    @Override
    public User requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.ADMIN) {
            throw new ResourceNotFoundException("Not found");
        }
        return user;
    }
}
