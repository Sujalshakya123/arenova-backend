package com.arenova.controllers;


import com.arenova.dtos.*;
import com.arenova.dtos.Auth.AuthResponse;
import com.arenova.dtos.Auth.ForgotPasswordRequest;
import com.arenova.dtos.Auth.LoginRequest;
import com.arenova.dtos.Auth.MessageResponse;
import com.arenova.dtos.Auth.OrganizerRegistrationStatusRequest;
import com.arenova.dtos.Auth.OrganizerRegistrationStatusResponse;
import com.arenova.dtos.Auth.RegisterRequest;
import com.arenova.dtos.Auth.RegisterResponse;
import com.arenova.dtos.Auth.ResetPasswordRequest;
import com.arenova.entities.User;
import com.arenova.mapper.UserMapper;
import com.arenova.respositories.UserRepository;
import com.arenova.security.AccountStatusSupport;
import com.arenova.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest request
    ) throws BadRequestException {

        return ResponseEntity.ok(
                authService.register(request)
        );
    }

        @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request){
            return authService.login(request);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Not authenticated."
            );
        }

        String email = authentication.getName();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow();

        AccountStatusSupport.requireUsable(user);

        return ResponseEntity.ok(
                UserMapper.toDTO(user)
        );
    }


    @PostMapping("/verify")
    public ResponseEntity<AuthResponse>
    verify(
            @RequestBody
            VerifyDTO request
    ) throws BadRequestException {

        return ResponseEntity.ok(
                authService.verify(request)
        );
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<String>
    resendOtp(
            @RequestBody
            ResendDTO request
    ) throws BadRequestException {

        return ResponseEntity.ok(
                authService.resendOtp(request)
        );
    }

    @PostMapping("/organizer-status")
    public ResponseEntity<OrganizerRegistrationStatusResponse> organizerStatus(
            @RequestBody OrganizerRegistrationStatusRequest request
    ) throws BadRequestException {
        return ResponseEntity.ok(authService.getOrganizerRegistrationStatus(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) throws BadRequestException {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/forgot-password/resend")
    public ResponseEntity<MessageResponse> resendForgotPasswordOtp(
            @RequestBody ForgotPasswordRequest request
    ) throws BadRequestException {
        return ResponseEntity.ok(authService.resendForgotPasswordOtp(request));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) throws BadRequestException {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

}
