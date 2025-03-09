package com.kwcapstone.Common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kwcapstone.Common.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"status", "code", "message", "data"})
public class BaseResponse<T> {
    private final int status;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

   /* public BaseResponse(int status, String message, T data) {
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
    }*/

    //객체 지향을 위해서임
    public static <T> BaseResponse<T> res(BaseCode code, T data){
        return new BaseResponse<>(code.getResponseHttpStauts().getHttpStatus().value(),
                code.getResponseHttpStauts().getMessage(),
                data);
    }
}
