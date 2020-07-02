package com.korniienko.kalah.service;

import com.korniienko.kalah.dao.UserRepository;
import com.korniienko.kalah.dto.UserCredentialsDto;
import com.korniienko.kalah.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
public class MyUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MyUserDetailsService myUserDetailsService;

    @Test
    public void shouldFindUserByUsernameWhenUserExist() {
        final User user = new User(1L, "admin", "admin", new ArrayList<>());
        Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        final UserDetails actualUser = myUserDetailsService.loadUserByUsername("admin");
        Assertions.assertEquals(user, actualUser);
    }

    @Test
    public void shouldFailToFindUserByUsernameWhenUserDoesNotExist() {
        Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> myUserDetailsService.loadUserByUsername("admin"));
        String expectedMessage = "Username [admin] not found.";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void shouldAddUserWhenUsernameIsUnique() {
        final User user = new User(1L, "admin", UUID.randomUUID().toString(), new ArrayList<>());
        Mockito.when(userRepository.save(any())).thenReturn(user);
        Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode("admin")).thenReturn("4DM1N");
        final User addedUser = myUserDetailsService.addUser(new UserCredentialsDto("admin", "admin"));
        Assertions.assertEquals(user, addedUser);
    }

    @Test
    public void shouldFailToAddUserWhenUsernameIsNotUnique() {
        final User user = new User(1L, "admin", "admin", new ArrayList<>());
        Mockito.when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        Exception exception = assertThrows(BadCredentialsException.class, () -> myUserDetailsService.addUser(new UserCredentialsDto("admin", "admin")));
        String expectedMessage = "User with name [admin] already exist, please use another username.";
        assertEquals(expectedMessage, exception.getMessage());
    }

}
