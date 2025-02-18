package com.kwcapstone.Kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

//Refresh Token 넣고 다시 accessToken 하고 refreshToken 발급받을 때 필요한 함수들
@Component
public class JwtTokenProvider {

    //필요한 필드
    //Http request의 Authorization 헤더에서 JWT 추출하기 위함
    private static final String  HEADER_STRING
            = "Authorization";
    //"Bearer" 로 시작하는 Jwt 토큰 처리 위한 값
    private static final String HEADER_STRING_PREFIX = " Bearer";

    private final SecretKey secretKey;//암호화키 -> 토큰이 변조되지 않았음을 검증
    private final long aTValidityMilliseconds; //AccessToken 유효기간
    private final long rTValidityMilliseconds; //RefrshToken 유효기간

    //생성자
    public JwtTokenProvider(
            @Value("")
    )
}
