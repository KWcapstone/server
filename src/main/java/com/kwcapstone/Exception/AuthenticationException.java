package com.kwcapstone.Exception;

public class AuthenticationException extends BaseException{
    //public BadRequestException(String message) { super(message); }
    public AuthenticationException(int code, String message) {
        super(code, message);
    }
}
