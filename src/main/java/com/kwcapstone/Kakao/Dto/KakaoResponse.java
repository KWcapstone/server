package com.kwcapstone.Kakao.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;

public class KakaoResponse {
    @Getter
    @Setter
    @NoArgsConstructor
    public static class KakaoLoginResponse{
        private String id;
        private String accessToken;

        public KakaoLoginResponse(ObjectId id, String accessToken) {
            this.id=id.toHexString();
            this.accessToken=accessToken;
        }
    }
}
