package com.korniienko.kalah.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    private String secretKey = "z5SJhPIQSeppkWLVuVcVGX7Mb7NnqZEPIK0pzas4U8zmBdixi21CJ6np5bf43mW";

    private long validityInMillis = 3600000; // 1h
}
