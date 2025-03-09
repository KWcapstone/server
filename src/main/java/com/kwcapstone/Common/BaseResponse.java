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
    private final String code; //어디서 오류 났는지 바로 알 수 있도록 추가함
    private final String message;

    //Json에서 data가 null 이면 자동으로 필드를 삭제하여 보내주는 역할
    //프론트가 싫으면 제거할 예정
    //@JsonInclude(JsonInclude.Include.NON_NULL)
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
                code.getResponseHttpStauts().getCode(),
                code.getResponseHttpStauts().getMessage(),
                data);
    }
}
