package com.kwcapstone.Common.code;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Getter
@AllArgsConstructor
@Builder
public class ResponseDTO {
    private HttpStatus httpStatus;
    private String code;
    private String message;
}
