package com.korniienko.kalah.dto;

import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
public class ErrorResponseDto {
    HttpStatus status;
    String message;
}
