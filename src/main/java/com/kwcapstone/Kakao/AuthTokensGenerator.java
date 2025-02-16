package com.kwcapstone.Kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokensGenerator {
    private static final String BEARER_TYPE = "Bearer";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000*60*60;//1시간
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000*60*60*24*14;//14일

    private final JwtTokenProvier
}
