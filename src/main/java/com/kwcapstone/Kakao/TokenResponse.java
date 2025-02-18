package com.kwcapstone.Kakao;

import lombok.*;

public class TokenResponse {
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Token {
        String accessToken;
        String refreshToken;
    }
}
