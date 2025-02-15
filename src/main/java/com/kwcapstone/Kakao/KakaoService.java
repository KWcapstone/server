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
            //Json 파싱에서 발생할 수 있는 오류 잡기
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AccessToken에서 Json 파싱 오류", e);
        }

        //json에서 access_token 값을 가져와 문자열로 변혼하여 반환
        return jsonNode.get("access_token").asText();
    }

    //토큰 -> 카카오 api 호출
    HashMap<String, Object> getKakaoUserInfo(String accessToken) {
        //userInfo(id,이름, 이메일)를 저장할 맵
        HashMap<String, Object> userInfo= new HashMap<String,Object>();

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        // responseBody에 있는 정보를 꺼냄
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "getUserInfo에서 Json 파싱 오류", e);
        }

        Long id = jsonNode.get("id").asLong();
        String email = jsonNode.get("kakao_account").get("email").asText();
        String nickname = jsonNode.get("Profile").get("nickname").asText();
        String imageUrl = jsonNode.get("Profile").get("profile_image_url").asText();

        userInfo.put("id",id);
        userInfo.put("email",email);
        userInfo.put("nickname",nickname);
        userInfo.put("imageUrl",imageUrl);

        return userInfo;
    }
    //카카오 ID로 회원가입 및 로그인 처리
    private KakaoResponse.KakaoLoginResponse kakaoLogin
    (HashMap<String, Object> userInfo){

        Long uid= Long.valueOf(userInfo.get("id").toString());
        String kakaoEmail = userInfo.get("email").toString();
        String nickName = userInfo.get("nickname").toString();
        String imageUrl = userInfo.get("imageUrl").toString();

        //이미 가입자인지 확인
        Boolean checkingKakaoUser = memberRepository.existsByEmail(kakaoEmail);

        if (!checkingKakaoUser) {//회원가입
            Member kakaoUser = new Member(uid,nickName,imageUrl,kakaoEmail,true);
            userRepository.save(kakaoUser);
        }
        //토큰 생성
        AuthTokens token=authTokensGenerator.generate(uid.toString());
        return new LoginResponse(uid,nickName,kakaoEmail,token);
    }

    //kakao login
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
