package com.arenova.services;

import com.arenova.respositories.UserRepository;
import com.arenova.security.AccountStatusSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {


    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       var user = repository.findByEmail(email)
               .orElseThrow(() -> new UsernameNotFoundException("Email Not Found"));

       boolean enabled = AccountStatusSupport.isUsable(user.getStatus());

       return org.springframework.security.core.userdetails.User
               .builder()
               .username(user.getEmail())
               .password(user.getPassword() != null ? user.getPassword() : "")
               .authorities("USER")
               .disabled(!enabled)
               .build();
    }
}
