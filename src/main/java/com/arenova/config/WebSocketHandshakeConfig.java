package com.arenova.config;

import com.arenova.security.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
public class WebSocketHandshakeConfig {

    @Bean
    public DefaultHandshakeHandler jwtHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(
                    org.springframework.http.server.ServerHttpRequest request,
                    org.springframework.web.socket.WebSocketHandler wsHandler,
                    Map<String, Object> attributes
            ) {
                Principal principal = JwtHandshakeInterceptor.principalFromAttributes(attributes);
                return principal != null ? principal : super.determineUser(request, wsHandler, attributes);
            }
        };
    }
}
