package com.kwcapstone.Common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode{
    //일반 회원 기능 관련
    USER_SIGN_UP(HttpStatus.OK, "AUTH2001", "회원가입이 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ResponseDTO getResponseHttpStauts(){
        return new ResponseDTO(httpStatus, code, message);
    }
}
