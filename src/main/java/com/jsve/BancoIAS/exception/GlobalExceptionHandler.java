package com.jsve.BancoIAS.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception) {

        ErrorResponse response = new ErrorResponse(
                exception.getStatus().value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }

    public record ErrorResponse(
            int status,
            String message,
            LocalDateTime timestamp
    ) {}
}
