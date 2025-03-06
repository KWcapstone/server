package com.kwcapstone.Common;

import com.kwcapstone.Exception.BaseException;
import lombok.Getter;

@Getter
public class BaseErrorResponse extends BaseResponse {
    public BaseErrorResponse(int status, String message) {
        super(status, message);
    }

    public BaseErrorResponse(BaseException baseException) {
        super(baseException.getCode(), baseException.getMessage());
    }
}
