package com.kwcapstone.Common;

import com.kwcapstone.Exception.BaseException;
import lombok.Getter;

@Getter
public class BaseErrorResponse {
    private final int status;
    private final String message;

    public BaseErrorResponse(int status, String messgae) {
        this.status = status;
        this.message = messgae;
    }

    public BaseErrorResponse(BaseException baseException) {
        this.status = baseException.getCode();
        this.message = baseException.getMessage();
    }
}
