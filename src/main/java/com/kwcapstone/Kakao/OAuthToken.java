package com.kwcapstone.Kakao;

import lombok.Data;

@Data
public class OAuthToken {
    private String access_token; //엑세스
    private String refresh_token; //리프레쉬
    private Long id_token; //토큰 Id값
    private Integer expires_in;//엑세스 만료시간
    private Integer refresh_expires_in;//리프레쉬 토큰 만료 시간
    private String token_type; //토큰타입
    private String scope; //인증된 사용자의 정보 조회 권한 범위
}
