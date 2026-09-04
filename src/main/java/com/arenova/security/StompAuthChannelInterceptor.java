package com.arenova.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user == null && accessor.getSessionAttributes() != null) {
                Object email = accessor.getSessionAttributes()
                        .get(JwtHandshakeInterceptor.USER_EMAIL_ATTR);
                if (email != null) {
                    user = () -> email.toString();
                    accessor.setUser(user);
                }
            }
            if (user == null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                user = principalFromConnectHeaders(accessor);
                if (user != null) {
                    accessor.setUser(user);
                    if (accessor.getSessionAttributes() != null) {
                        accessor.getSessionAttributes().put(
                                JwtHandshakeInterceptor.USER_EMAIL_ATTR,
                                user.getName()
                        );
                    }
                }
            }
            if (user != null) {
                setSecurityContext(user.getName());
            }
        }

        return message;
    }

    private Principal principalFromConnectHeaders(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String header = authHeaders.get(0);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        if (!jwtService.isTokenValid(token)) {
            return null;
        }
        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            return null;
        }
        return () -> email;
    }

    private void setSecurityContext(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
