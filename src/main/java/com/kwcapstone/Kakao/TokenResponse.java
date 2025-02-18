package com.kwcapstone.Kakao;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenResponse {
    String accessToken;
    String refreshToken;
}
