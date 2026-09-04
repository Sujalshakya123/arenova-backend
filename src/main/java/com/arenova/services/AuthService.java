package com.arenova.services;


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
import com.arenova.dtos.enums.AuthProvider;
import com.arenova.dtos.enums.Role;
import com.arenova.dtos.enums.UserStatus;
import com.arenova.entities.PasswordResetOtp;
import com.arenova.entities.PendingRegistration;
import com.arenova.entities.User;
import com.arenova.mapper.UserMapper;
import com.arenova.respositories.PasswordResetOtpRepository;
import com.arenova.respositories.PendingRegistrationRepository;
import com.arenova.respositories.UserRepository;
import com.arenova.security.AccountStatusSupport;
import com.arenova.security.JwtService;
import com.arenova.security.OtpService;
import com.arenova.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final PendingRegistrationRepository pendingRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final OtpService otpService;
    private final EmailService emailService;


    //Registration


    public RegisterResponse register(RegisterRequest request) throws BadRequestException {

        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException(
                    "Email already exists."
            );
        }

        PasswordPolicy.requireStrongPassword(request.getPassword());

        if (request.getRole() == null) {
            throw new BadRequestException("Role is required.");
        }

        String primaryGame = normalizePrimaryGame(request.getPrimaryGame());
        if (request.getRole() == Role.PLAYER && primaryGame == null) {
            throw new BadRequestException("Primary game is required for player registration.");
        }
        if (request.getRole() == Role.ORGANIZER && primaryGame != null) {
            primaryGame = null;
        }

        pendingRepository.deleteByEmail(request.getEmail());

        String otp = otpService.generateOtp();

        PendingRegistration pending =
                PendingRegistration.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .role(request.getRole())
                        .primaryGame(primaryGame)
                        .authProvider(AuthProvider.LOCAL)
                        .otp(otp)
                        .expiryTime(LocalDateTime.now().plusMinutes(5))
                        .attempts(0)
                        .resendAvailableAt(
                                LocalDateTime.now()
                                        .plusSeconds(60)
                        )
                        .build();

        pendingRepository.save(pending);

        emailService.sendOtp(
                request.getEmail(),
                otp
        );

        return RegisterResponse.builder()
                .success(true)
                .message("Register Success. Check Email for OTP Code")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public AuthResponse verify(VerifyDTO request) throws BadRequestException {

        PendingRegistration pending = pendingRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("OTP not found."));

        if (pending.getExpiryTime()
                .isBefore(
                        LocalDateTime.now()
                )) {
            throw new BadRequestException(
                    "OTP expired."
            );
        }

        if (pending.getAttempts() >= 5) {

            pendingRepository.delete(
                    pending
            );

            throw new BadRequestException(
                    "Maximum verification attempts exceeded. Please register again."
            );
        }

        if (!pending.getOtp()
                .equals(request.getOtp())) {

            pending.setAttempts(
                    pending.getAttempts() + 1
            );

            pendingRepository.save(
                    pending
            );

            throw new BadRequestException(
                    "Invalid OTP. Remaining attempts: "
                            + (5 - pending.getAttempts())
            );
        }

        boolean organizerRegistration = pending.getRole() == Role.ORGANIZER;
        UserStatus initialStatus = organizerRegistration ? UserStatus.PENDING : UserStatus.ACTIVE;

        User user =
                User.builder()
                        .username(pending.getUsername())
                        .email(pending.getEmail())
                        .password(pending.getPassword())
                        .role(pending.getRole())
                        .preferredGames(
                                pending.getRole() == Role.PLAYER ? pending.getPrimaryGame() : null
                        )
                        .authProvider(AuthProvider.LOCAL)
                        .status(initialStatus)
                        .build();

        repository.save(user);

        pendingRepository.delete(pending);

        if (organizerRegistration) {
            return AuthResponse.builder()
                    .userDTO(UserMapper.toDTO(user))
                    .pendingApproval(true)
                    .message(
                            "Registration successful. Your organizer account is waiting for Super Admin approval."
                    )
                    .build();
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userDTO(UserMapper.toDTO(user))
                .pendingApproval(false)
                .build();
    }

    public String resendOtp(
            ResendDTO request
    ) throws BadRequestException {

        PendingRegistration pending =
                pendingRepository
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Registration not found."
                                )
                        );

        if (pending.getResendAvailableAt()
                .isAfter(LocalDateTime.now())) {

            long secondsRemaining =
                    Duration.between(
                            LocalDateTime.now(),
                            pending.getResendAvailableAt()
                    ).getSeconds();

            throw new BadRequestException(
                    "Please wait "
                            + secondsRemaining
                            + " seconds before requesting another OTP."
            );
        }

        String otp =
                otpService.generateOtp();

        pending.setOtp(otp);

        pending.setAttempts(0);

        pending.setExpiryTime(
                LocalDateTime.now()
                        .plusMinutes(5)
        );

        pending.setResendAvailableAt(
                LocalDateTime.now()
                        .plusSeconds(60)
        );

        pendingRepository.save(pending);

        emailService.sendOtp(
                pending.getEmail(),
                otp
        );

        return "OTP resent successfully.";
    }

    public AuthResponse login(LoginRequest request) {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Wrong email or password."));

        AccountStatusSupport.requireLoginAllowed(user);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userDTO(UserMapper.toDTO(user))
                .build();
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) throws BadRequestException {
        String email = normalizeEmail(request.getEmail());
        if (email.isEmpty()) {
            throw new BadRequestException("Email is required.");
        }

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found with this email."));

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new BadRequestException(
                    "This account signs in with Google. Password reset is not available."
            );
        }

        AccountStatusSupport.requireUsable(user);

        createAndSendPasswordResetOtp(email);
        return MessageResponse.builder()
                .success(true)
                .message("OTP sent to your email. It expires in 5 minutes.")
                .build();
    }

    @Transactional
    public MessageResponse resendForgotPasswordOtp(ForgotPasswordRequest request) throws BadRequestException {
        String email = normalizeEmail(request.getEmail());
        if (email.isEmpty()) {
            throw new BadRequestException("Email is required.");
        }

        PasswordResetOtp pending = passwordResetOtpRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No password reset request found. Start again."));

        if (pending.getResendAvailableAt() != null
                && pending.getResendAvailableAt().isAfter(LocalDateTime.now())) {
            long secondsRemaining = Duration.between(
                    LocalDateTime.now(),
                    pending.getResendAvailableAt()
            ).getSeconds();
            throw new BadRequestException(
                    "Please wait " + secondsRemaining + " seconds before requesting another OTP."
            );
        }

        createAndSendPasswordResetOtp(email);
        return MessageResponse.builder()
                .success(true)
                .message("OTP resent successfully.")
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) throws BadRequestException {
        String email = normalizeEmail(request.getEmail());
        String otp = request.getOtp() == null ? "" : request.getOtp().trim();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword();

        if (email.isEmpty()) {
            throw new BadRequestException("Email is required.");
        }
        if (otp.isEmpty()) {
            throw new BadRequestException("OTP is required.");
        }
        PasswordPolicy.requireStrongPassword(newPassword);

        PasswordResetOtp pending = passwordResetOtpRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("OTP not found. Request a new code."));

        if (pending.getExpiryTime() != null && pending.getExpiryTime().isBefore(LocalDateTime.now())) {
            passwordResetOtpRepository.delete(pending);
            throw new BadRequestException("OTP expired. Request a new code.");
        }

        int attempts = pending.getAttempts() == null ? 0 : pending.getAttempts();
        if (attempts >= 5) {
            passwordResetOtpRepository.delete(pending);
            throw new BadRequestException("Too many invalid attempts. Request a new code.");
        }

        if (!otp.equals(pending.getOtp())) {
            pending.setAttempts(attempts + 1);
            passwordResetOtpRepository.save(pending);
            int remaining = 5 - pending.getAttempts();
            throw new BadRequestException("Invalid OTP. Remaining attempts: " + remaining);
        }

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found with this email."));

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            passwordResetOtpRepository.delete(pending);
            throw new BadRequestException(
                    "This account signs in with Google. Password reset is not available."
            );
        }

        AccountStatusSupport.requireUsable(user);

        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        passwordResetOtpRepository.delete(pending);

        return MessageResponse.builder()
                .success(true)
                .message("Password updated successfully. You can log in now.")
                .build();
    }

    public OrganizerRegistrationStatusResponse getOrganizerRegistrationStatus(
            OrganizerRegistrationStatusRequest request
    ) throws BadRequestException {
        String email = normalizeEmail(request.getEmail());
        if (email.isEmpty()) {
            throw new BadRequestException("Email is required.");
        }

        return repository.findByEmail(email)
                .filter(user -> user.getRole() == Role.ORGANIZER)
                .map(user -> {
                    UserStatus status = user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE;
                    return OrganizerRegistrationStatusResponse.builder()
                            .found(true)
                            .email(user.getEmail())
                            .status(status)
                            .message(organizerStatusMessage(status))
                            .build();
                })
                .orElseGet(() -> OrganizerRegistrationStatusResponse.builder()
                        .found(false)
                        .message("No organizer registration found for this email.")
                        .build());
    }

    private String organizerStatusMessage(UserStatus status) {
        if (status == UserStatus.PENDING || status == UserStatus.INACTIVE) {
            return "Your organizer account is waiting for Super Admin approval.";
        }
        if (status == UserStatus.REJECTED) {
            return "Your organizer registration was rejected. Contact the administrator for more information.";
        }
        if (status == UserStatus.SUSPENDED) {
            return "Your organizer account has been suspended. Contact support for help.";
        }
        return "Your organizer account is approved. You can log in and access the dashboard.";
    }

    private void createAndSendPasswordResetOtp(String email) {
        passwordResetOtpRepository.deleteByEmail(email);

        String otp = otpService.generateOtp();
        PasswordResetOtp pending = PasswordResetOtp.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .resendAvailableAt(LocalDateTime.now().plusSeconds(60))
                .build();
        passwordResetOtpRepository.save(pending);
        emailService.sendPasswordResetOtp(email, otp);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }

    private String normalizePrimaryGame(String primaryGame) {
        if (primaryGame == null) {
            return null;
        }
        String trimmed = primaryGame.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
