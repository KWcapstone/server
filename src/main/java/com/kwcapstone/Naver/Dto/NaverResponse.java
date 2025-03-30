package com.kwcapstone.Naver.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

public class NaverResponse {
    @Getter
    @Setter
    @NoArgsConstructor
    public static class NaverLoginResponse{
        private String id;
        private String accessToken;
        private String refreshToken;

        public NaverLoginResponse(ObjectId id, String accessToken, String refreshToken) {
            this.id = id.toHexString();
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
