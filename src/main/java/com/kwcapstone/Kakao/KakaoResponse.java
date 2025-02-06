package com.kwcapstone.Kakao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class KakaoResponse {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class KakaoLoginResponse{
        private String name;
        private String imageUrl;
        private String email;
    }
}
