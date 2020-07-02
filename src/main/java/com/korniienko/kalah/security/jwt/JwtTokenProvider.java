package com.korniienko.kalah.security.jwt;

import com.korniienko.kalah.dao.UserRepository;
import com.korniienko.kalah.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private String secretKey;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties, UserRepository userRepository) {
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
    }

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(jwtProperties.getSecretKey().getBytes());
    }

    public String createToken(String username, List<String> authorities) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("authorities", authorities);

        Date now = new Date();
        Date validity = Date.from(Instant.ofEpochMilli(now.getTime() + jwtProperties.getValidityInMillis()));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)), SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        final String username = getUsername(token);
        final User user = userRepository.findByUsername(username).orElseThrow(usernameNotFound(username));
        return new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
    }

    private Supplier<UsernameNotFoundException> usernameNotFound(String username) {
        return () -> new UsernameNotFoundException(String.format("Username [%s]] not found", username));
    }

    public String getUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = parseToken(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
    }

}
