package com.korniienko.kalah.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korniienko.kalah.dao.UserRepository;
import com.korniienko.kalah.dto.UserCredentialsDto;
import com.korniienko.kalah.model.User;
import com.korniienko.kalah.security.jwt.JwtAuthenticationEntryPoint;
import com.korniienko.kalah.security.jwt.JwtTokenProvider;
import com.korniienko.kalah.service.GameService;
import com.korniienko.kalah.service.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthenticationController.class)
@ActiveProfiles("test")
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private GameService gameService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private MyUserDetailsService userDetailsService;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void shouldLoginExistingUserAndReturnJwtToken() throws Exception {
        String expectedToken = UUID.randomUUID().toString();
        final User user = new User(1L, "admin", "admin", new ArrayList<>());
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(user);
        when(jwtTokenProvider.createToken(anyString(), anyList())).thenReturn(expectedToken);
        final byte[] content = objectMapper.writeValueAsBytes(new UserCredentialsDto("admin", "admin"));
        this.mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is(expectedToken)))
                .andExpect(jsonPath("$.username", is(user.getUsername())));
    }

    @Test
    public void shouldFailToLoginNonExistingUserAndReturnErrorResponse() throws Exception {
        final UsernameNotFoundException ex = new UsernameNotFoundException("Username [SPRING_TEST] not found.");
        when(userDetailsService.loadUserByUsername(anyString())).thenThrow(ex);
        final byte[] content = objectMapper.writeValueAsBytes(new UserCredentialsDto("admin", "admin"));
        this.mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.name())))
                .andExpect(jsonPath("$.message", is(ex.getMessage())));
    }

    @Test
    public void shouldRegisterNewUserAndReturnJwtToken() throws Exception {
        String expectedToken = UUID.randomUUID().toString();
        final User user = new User(1L, "admin", "admin", new ArrayList<>());
        final UserCredentialsDto credentialsDto = new UserCredentialsDto("admin", "admin");
        when(userDetailsService.addUser(credentialsDto)).thenReturn(user);
        when(jwtTokenProvider.createToken(anyString(), anyList())).thenReturn(expectedToken);
        final byte[] content = objectMapper.writeValueAsBytes(credentialsDto);
        this.mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is(expectedToken)))
                .andExpect(jsonPath("$.username", is(user.getUsername())));
    }

    @Test
    public void shouldFailToRegisterNewUserWithNonUniqueNameAndReturnErrorResponse() throws Exception {
        final BadCredentialsException ex = new BadCredentialsException("User with name [SPRING_TEST] already exist, please use another username.");
        final UserCredentialsDto userCredentialsDto = new UserCredentialsDto("admin", "admin");
        when(userDetailsService.addUser(userCredentialsDto)).thenThrow(ex);
        final byte[] content = objectMapper.writeValueAsBytes(userCredentialsDto);
        this.mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.name())))
                .andExpect(jsonPath("$.message", is(ex.getMessage())));
    }
}
