package com.kwcapstone.Common.Response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ResponseDTO {
    private HttpStatus httpStatus;
    private String message;
}
