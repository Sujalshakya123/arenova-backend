package com.arenova.controllers;


import com.arenova.dtos.AuthResponse;
import com.arenova.dtos.LoginRequest;
import com.arenova.dtos.RegisterRequest;
import com.arenova.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

        @PostMapping("/register")
        public AuthResponse register(@Valid @RequestBody RegisterRequest request){
            return authService.register(request);
        }

        @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request){
            return authService.login(request);
    }
}
