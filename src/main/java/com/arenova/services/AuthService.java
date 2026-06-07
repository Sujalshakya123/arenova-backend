package com.arenova.services;


import com.arenova.dtos.AuthResponse;
import com.arenova.dtos.LoginRequest;
import com.arenova.dtos.RegisterRequest;
import com.arenova.dtos.enums.AuthProvider;
import com.arenova.dtos.enums.Role;
import com.arenova.entities.User;
import com.arenova.mapper.UserMapper;
import com.arenova.respositories.UserRepository;
import com.arenova.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public AuthResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email Already Exists");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException("Cannot register as Administrator!");
        }else{
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole())
                    .authProvider(AuthProvider.LOCAL)
                    .build();

            repository.save(user);

            String token = jwtService.generateToken(user);
            return AuthResponse.builder()
                    .token(token)
                    .userDTO(UserMapper.toDTO(user))
                    .build();
        }
    }





    public AuthResponse login(LoginRequest request){

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(()-> new UsernameNotFoundException("User with given email is not found: " + request.getEmail()));

        String token = jwtService.generateToken(user);
     return  AuthResponse.builder()
                .token(token)
                .userDTO(UserMapper.toDTO(user))
                .build();
    }

}
