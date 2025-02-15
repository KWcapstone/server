package com.kwcapstone.Kakao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoService {
    private final MemberRepository memberRepository;

    @Value("${kakao.key.client-id}")
    private String clientId;

    @Value("${kakao.redirect-url")
    private String redirectUrl;

    //엑세스 토큰 요청
    private String getAccessToken(String code) {
        //Http Header 생성
        HttpHeaders headers = new HttpHeaders();
        //카카오 api 요청 시, 서버가 요청의 형식을 이해할 수 있도록 하는 것
        //client와 카카오 서버 간의 통신을 원활하게 하기 위해
        headers.set("Authorization", "Bearer " + code);

        // HTTP Body 생성(엑세스 토큰을 요청하기 위해 필요한 필수 데이터)
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("code", code);

        //Http 요청 보내기
        //1. HttpEntity 객체 생성(요청 데이터 감싼거임)
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest
                = new HttpEntity<>(body, headers);
        //2. Http 요청을 보낼 도구인 Resttemplate 객체 생성
        RestTemplate rt = new RestTemplate();
        //3. Http 요청보내기
        ResponseEntity<String> response
                = rt.exchange("https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class);

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        //1. 위 http 응답 데이터 가져오기
        String responseBody = response.getBody();
        //2. Json 문자열을 객체로 변환(Json 파싱)
        ObjectMapper objectMapper = new ObjectMapper();//Json 파싱 도구
        JsonNode jsonNode = null; //Json 데이터를 트리 구조로 저장하는 객체
        try {
            jsonNode = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e){
            e.printStackTrace();
        }

        //json에서 access_token 값을 가져와 문자열로 변혼하여 반환
        return jsonNode.get("access_token").asText();
    }
    //토큰 -> 카카오 api 호출

    //카카오 ID로 회원가입 및 로그인 처리

    //kakao login
    public KakaoResponse.KakaoLoginResponse kakaoLogin(String code){
        //인가코드 -> 엑세스 토큰 요청
        String accessToken = getAccessToken(code);

        //토큰 -> 카카오 api 호출
        HashMap<String, Optional> userInfo = getKakaoUserInfo(accessToken);

        //카카오 ID로 회원가입 및 로그인 처리
        KakaoResponse.KakaoLoginResponse response
                = kakaoUserLogin(userInfo);

        return response;
    }

}
