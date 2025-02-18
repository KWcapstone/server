package com.kwcapstone.Kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KaKaoProvider {
    //필요한 필드값
    @Value("${KAKAO_CLIENT_ID}")
    private String clientId;

    @Value("${KAKAO_REDIRECT_URL}")
    private String redirectUrl;

    //code로 accessToken 요청하기(해당 accessToken은 카카오에서 제공해주는 token)
    //보안을 위해 accessToken을 새로 서버쪽에서 발급할거임.
    public
}
