package com.kwcapstone.Kakao;


public class TokenConvert {
    public static TokenResponse toTokenRefreshResponse(
            String accessToken, String refreshToken) {
        return TokenResponse
                .builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
