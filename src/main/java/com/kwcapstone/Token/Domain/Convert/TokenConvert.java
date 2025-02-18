package com.kwcapstone.Token.Domain.Convert;


import com.kwcapstone.Token.Domain.Dto.TokenResponse;

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
