package com.example.taskmanager.service;

import com.example.taskmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        // FIX: .roles("USER") and .authorities("ROLE_USER") are equivalent,
                        // but being explicit here avoids confusion with SimpleGrantedAuthority.
                        // Both produce "ROLE_USER" — this is intentionally kept consistent
                        // with Spring Security's hasRole("USER") check.
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}