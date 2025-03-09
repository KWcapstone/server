package com.kwcapstone.Common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode{
    //일반 회원 기능 관련
    USER_SIGN_UP(HttpStatus.OK, "회원가입이 완료되었습니다."),

    //이메일 중복확인
    USER_EMAIL_DUPLICATION(HttpStatus.OK, "사용 가능한 이메일"),
    USER_EMAIL_VERIFICATION(HttpStatus.OK, "이메일 인증이 완료되었습니다."),

    //소셜로그인
    USER_
    ;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public ResponseDTO getResponseHttpStauts(){
        return new ResponseDTO(httpStatus,message);
    }
}
