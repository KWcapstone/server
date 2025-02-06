package com.kwcapstone.Kakao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class KakaoResponse {
    @Getter
    @Setter
    @AllArgsConstructor
    public static class KakaoLoginResponse{
        private final String name;
        private final String imageUrl;
        private final String email;
    }
}
