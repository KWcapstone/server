package com.kwcapstone.Kakao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

public class KakaoResponse {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoLoginResponse{
        private ObjectId id;
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoTokenResponse{
        private String accessToken;
        private String refreshToken;
    }
}
