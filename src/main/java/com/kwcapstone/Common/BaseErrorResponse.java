package com.kwcapstone.Common;

import com.kwcapstone.Exception.BaseException;
import lombok.Getter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class BaseErrorResponse extends BaseResponse {
    public BaseErrorResponse(int status, String message) {
        super(status, message);
    }

    public BaseErrorResponse(BaseException baseException) {
        super(baseException.getCode(), baseException.getMessage());
    }
}
