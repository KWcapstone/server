package com.kwcapstone.Exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseException extends RuntimeException {
    private final int code;
    private final String message;

    public BaseException(final int code, final String message) {
        this.code = code;
        this.message = message;
    }

    public int getStatusCode(){
        return this.getCode();
    }
}
