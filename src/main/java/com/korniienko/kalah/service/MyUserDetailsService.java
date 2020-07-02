package com.korniienko.kalah.service;

import com.korniienko.kalah.dao.UserRepository;
import com.korniienko.kalah.dto.UserCredentialsDto;
import com.korniienko.kalah.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Component
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MyUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(usernameNotFound(username));
    }

    public User addUser(UserCredentialsDto userCredentials) {
        userRepository.findByUsername(userCredentials.getUsername()).map((user) -> {
            final String message = String.format("User with name [%s] already exist, please use another username.", user.getUsername());
            throw new BadCredentialsException(message);
        });
        final List<String> roles = Collections.singletonList("ROLE_ADMIN");
        final User entity = new User(null, userCredentials.getUsername(), passwordEncoder.encode(userCredentials.getPassword()), roles);
        return userRepository.save(entity);
    }

    private Supplier<UsernameNotFoundException> usernameNotFound(String username) {
        return () -> new UsernameNotFoundException(String.format("Username [%s] not found.", username));
    }
}
