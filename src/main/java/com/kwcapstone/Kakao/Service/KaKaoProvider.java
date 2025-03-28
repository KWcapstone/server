package com.kwcapstone.Kakao.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Kakao.Dto.KaKaoProfile;
import com.kwcapstone.Token.Domain.Dto.OAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KaKaoProvider {
    //필요한 필드값
    @Value("${KAKAO_CLIENT_ID}")
    private String clientId;

    @Value("${KAKAO_REDIRECT_URI}")
    private String redirectUri;
    @Value("${KAKAO_CLIENT_SECRET}")
    private String clientSecret;

    //code(인가코드)로 accessToken 요청하기
    // (해당 accessToken은 카카오에서 제공해주는 token)
    //보안을 위해 accessToken을 새로 서버쪽에서 발급할거임.
    public OAuthToken requestToken(String code){
        //Restemplate 새로 생성(Http 요청을 보내기 위함)
        RestTemplate restTemplate = new RestTemplate();

        //Http 요청의 header에 방식(Json이 아닌 URL 인코딩된 폼 데이터)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type",
                "application/x-www-form-urlencoded;charset=utf-8");

        //넘길 정보 담기
        MultiValueMap<String, String> params
                = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("client_secret", clientSecret);

        //요청할 객체 생성
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest
                = new HttpEntity<>(params, headers);

        //카카오 OAuth 토큰 요청하기
        ResponseEntity<String> response
                = restTemplate.exchange(
                        "https://kauth.kakao.com/oauth/token", HttpMethod.POST, kakaoTokenRequest, String.class);

        //응답데이터는 OAuthToken로 변환해야함(이를 위해 객체 매핑할 도구)
        ObjectMapper objectMapper = new ObjectMapper();
        OAuthToken oauthToken = null;

        try{
            oauthToken = objectMapper.readValue(
                    response.getBody(),OAuthToken.class);
        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "카카오 정보 불러오기에 실패하였습니다.");
        }
        return oauthToken;
    }

    //Token으로 정보 요청
    public KaKaoProfile getProfile(String token){
        //Http 요청을 위해
        RestTemplate restTemplate
                = new RestTemplate();

        //Http 요청 담기 위해
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type",
                "application/x-www-form-urlencoded;charset=utf-8");

        //넘길정보 담기
        MultiValueMap<String, Object> params
                = new LinkedMultiValueMap<>();
        params.add("secure_resource", true);

        //Http 요청하기
        HttpEntity<MultiValueMap<String, Object>> kakaoProfileRequest
                = new HttpEntity<>(params, headers);

        //정보 받아오기
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "https://kapi.kakao.com/v2/user/me",
                        HttpMethod.POST,
                        kakaoProfileRequest, String.class);

        //객체 매핑 생성
        ObjectMapper objectMapper = new ObjectMapper();
        KaKaoProfile kaKaoProfile = null;

        try{
            kaKaoProfile
                    = objectMapper.readValue(response.getBody(), KaKaoProfile.class);
        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "카카오 유저 정보 불러오기에 실패하였습니다.");
        }

        return kaKaoProfile;
    }
}
