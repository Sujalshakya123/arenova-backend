package com.arenova.security;

import com.arenova.entities.User;
import com.arenova.respositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        OAuth2User oauthUser =
                (OAuth2User)
                        authentication.getPrincipal();

        String email =
                oauthUser.getAttribute("email");

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow();

        if (!AccountStatusSupport.isUsable(user.getStatus())) {
            String message = AccountStatusSupport.blockedMessage(user.getStatus());
            String redirect = UriComponentsBuilder
                    .fromUriString("http://localhost:5173/login")
                    .queryParam("error", message)
                    .encode()
                    .build()
                    .toUriString();
            response.sendRedirect(redirect);
            return;
        }

        String token =
                jwtService.generateToken(user);

        response.sendRedirect(
                "http://localhost:5173" +
                        "/oauth-success?token=" +
                        token
        );
    }
}
