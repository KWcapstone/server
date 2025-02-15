package com.kwcapstone.Kakao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class KakaoResponse {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoLoginResponse{
        private Long id;
        private String name;
        private String imageUrl;
        private String email;
    }
}
