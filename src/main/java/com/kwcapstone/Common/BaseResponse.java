package com.kwcapstone.Common;

import lombok.Getter;

@Getter
public class BaseResponse<T> {
    private final int status;
    private String message;
    private T data;

    public BaseResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public BaseResponse(int status, T data) {
        this.status = status;
        this.data = data;
    }
}
