package com.korniienko.kalah.controller;

import com.korniienko.kalah.dto.UserCredentialsDto;
import com.korniienko.kalah.dto.UserTokenDto;
import com.korniienko.kalah.security.jwt.JwtTokenProvider;
import com.korniienko.kalah.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final MyUserDetailsService userDetailsService;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, MyUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserTokenDto> login(@RequestBody UserCredentialsDto data) {
        String username = data.getUsername();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.getPassword()));
        final UserDetails user = userDetailsService.loadUserByUsername(username);
        final List<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        String token = jwtTokenProvider.createToken(username, authorities);
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserTokenDto(username, token));
    }

    @PostMapping("/signup")
    public ResponseEntity<UserTokenDto> signup(@RequestBody UserCredentialsDto data) {
        String username = data.getUsername();
        final UserDetails user = userDetailsService.addUser(data);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.getPassword()));
        final List<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        String token = jwtTokenProvider.createToken(username, authorities);
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserTokenDto(username, token));
    }

}
