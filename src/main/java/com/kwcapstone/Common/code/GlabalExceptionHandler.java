package com.kwcapstone.Common.code;

import com.kwcapstone.Common.BaseErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlabalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BaseErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new BaseErrorResponse(ex.getStatusCode().value(), ex.getMessage()));
    }
}
