package com.kwcapstone.Common.Response;

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

    //토큰 발급
    USER_REISSUE_TOKEN(HttpStatus.OK,"토큰 재발급이 완료되었습니다."),

    //소셜로그인
    USER_GOOGLE_LOGIN(HttpStatus.OK,"로그인 성공"),
    USER_NEW_GOOGLE_LOGIN(HttpStatus.OK,"로그인 성공"),
    USER_KAKAO_LOGIN(HttpStatus.OK,"로그인 성공"),
    USER_NAVER_LOGIN(HttpStatus.OK,"로그인 성공"),

    //비밀번호 초기화 및 변경
    USER_RESET_PW(HttpStatus.OK,"이메일에 발송된 비밀번호를 확인하세요."),
    USER_AlREADY_SOCIAL_LOGIN(HttpStatus.OK,"소셜 로그인으로 가입돈 이메일입니다. 일반 로그인이 아닌 소셜 로그인을 사용해 주세요."),

    //비밀번호 변경
    USER_PW_PATCH(HttpStatus.OK, "비밀번호 변경이 완료되었습니다."),

    //로그아웃
    USER_LOGOUT(HttpStatus.OK,"로그아웃이 완료되었습니다."),

    //회원탈퇴
    USER_WITHDRAW(HttpStatus.OK, "회원 탈퇴가 완료되었습니다."),

    // 알림창 조회
    NOTICE_CONFIRM(HttpStatus.OK, "모든 알림을 조회합니다."),

    // 알림창 세부 조회
    NOTICE_DETAIL_CONFIRM(HttpStatus.OK, "알림 세부 조회입니다."),

    // 메인화면 확인
    MAIN_SHOW(HttpStatus.OK, "모든 프로젝트 리스트를 불러왔습니다."),

    // 메인화면 녹음파일 + 스크립트 확인
    MAIN_RECORDING(HttpStatus.OK, "모든 녹음 리스트를 불러왔습니다."),

    // 메인화면 요약본 확인
    MAIN_SUMMARY(HttpStatus.OK, "모든 요약본 리스트를 불러왔습니다."),

    // 탭별로 검색
    MAIN_SEARCH(HttpStatus.OK, "프로젝트 조회 결과입니다."),

    //프로필 조회
    SHOW_PROFILE(HttpStatus.OK, "프로필를 조회했습니다."),

    //api test
    API_TEST(HttpStatus.OK, "API TEST에 성공하였습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public ResponseDTO getResponseHttpStauts(){
        return new ResponseDTO(httpStatus,message);
    }
}
