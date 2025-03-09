package com.kwcapstone.Common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode{
    //일반 회원 기능 관련
    USER_SIGN_UP(HttpStatus.OK, "회원가입이 완료되었습니다."),
    USER_LOGIN(HttpStatus.OK, "로그인이 완료되었습니다."),

    //이메일 중복확인
    USER_EMAIL_DUPLICATION(HttpStatus.OK, "사용 가능한 이메일"),
    USER_EMAIL_VERIFICATION(HttpStatus.OK, "이메일 인증이 완료되었습니다."),

    //소셜로그인
    USER_GOOGLE_LOGIN(HttpStatus.OK,"로그인 성공"),
    USER_NEW_GOOGLE_LOGIN(HttpStatus.OK,"로그인 성공"),
    USER_KAKAO_LOGIN(HttpStatus.OK,"로그인 성공"),

    //비밀번호 초기화 및 변경
    USER_RESET_PW(HttpStatus.OK,"이메일에 발송된 비밀번호를 확인하세요."),
    USER_AlREADY_SOCIAL_LOGIN(HttpStatus.OK,"소셜 로그인으로 가입돈 이메일입니다. 일반 로그인이 아닌 소셜 로그인을 사용해 주세요."),

    //로그아웃
    USER_LOGOUT(HttpStatus.OK,"로그아웃이 완료되었습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public ResponseDTO getResponseHttpStauts(){
        return new ResponseDTO(httpStatus,message);
    }
}
