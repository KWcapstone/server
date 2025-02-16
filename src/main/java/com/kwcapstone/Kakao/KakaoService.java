package com.kwcapstone.Kakao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoService {
    private final MemberRepository memberRepository;

    //kakao login
    @Transactional
    public KakaoResponse.KakaoLoginResponse kakaoLogin(String code){
        //인가코드 -> 엑세스 토큰 요청
        String accessToken = getAccessToken(code);

        //토큰 -> 카카오 api 호출
        HashMap<String, Object> userInfo = getKakaoUserInfo(accessToken);

        //카카오 ID로 회원가입 및 로그인 처리
        KakaoResponse.KakaoLoginResponse response
                = kakaoUserLogin(userInfo);

        return response;
    }
}
