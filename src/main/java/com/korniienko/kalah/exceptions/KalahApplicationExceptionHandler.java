package com.korniienko.kalah.exceptions;

import com.korniienko.kalah.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class KalahApplicationExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = IllegalMoveException.class)
    protected ResponseEntity<Object> handleIllegalMove(RuntimeException ex, WebRequest request) {
        logger.debug(ex.getMessage(), ex);
        final ErrorResponseDto body = new ErrorResponseDto(HttpStatus.BAD_REQUEST, ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = {GameNotFoundException.class, UsernameNotFoundException.class})
    protected ResponseEntity<Object> handleEntityNotFound(RuntimeException ex, WebRequest request) {
        log.debug(ex.getMessage(), ex);
        final ErrorResponseDto body = new ErrorResponseDto(HttpStatus.NOT_FOUND, ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = {BadCredentialsException.class})
    protected ResponseEntity<Object> handleBadCredentialsException(RuntimeException ex, WebRequest request) {
        log.debug(ex.getMessage(), ex);
        final ErrorResponseDto body = new ErrorResponseDto(HttpStatus.BAD_REQUEST, ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = {RuntimeException.class})
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.debug(ex.getMessage(), ex);
        final ErrorResponseDto body = new ErrorResponseDto(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
