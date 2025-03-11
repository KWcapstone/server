package com.kwcapstone.Naver.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Token.Domain.Dto.OAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@Component
@RequiredArgsConstructor
public class NaverProvider {
    //필요한 필드값
    @Value("${NAVER_CLIENT_ID}")
    private String clientId;

    @Value("${NAVER_REDIRECT_URI}")
    private String redirectUri;

    @Value("${NAVER_CLIENT_SECRET}")
    private String clientSecret;

    //code(인가코드)로 accessToken 요청하기
    public OAuthToken requestToken(String code){
        //code가 null 이거나 빈 공백인 경우의 예외처리
        if(code == null | code.trim().isEmpty()){
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "code 값이 null 입니다.");
        }

        RestTemplate restTemplate = new RestTemplate();

        //Http 요청에서의 header 정보
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type",
                "application/x-www-form-urlencoded;charset=UTF-8");

        //넘길 정보 담기
        MultiValueMap<String, String> params
                = new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        //요청할 객체 새서
        HttpEntity<MultiValueMap<String,String>> naverTokenRequest
                =new HttpEntity<>(params, headers);

        //네이버 OAuth 토큰 요청하기
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    "https://nid.naver.com/oauth2.0/token", HttpMethod.GET,
                    naverTokenRequest, String.class);
        }catch (RestClientException e){
            //ResTemplate 에서 Http 요청을 보내는 중 발생하는 예외(서버 문제임)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "네이버 서버가 응답하지 않습니다.");
        }

        //응답데이터는 OAuthToken으로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        OAuthToken oAuthToken = null;

        try{
            oAuthToken = objectMapper.readValue(
                    response.getBody(),OAuthToken.class);
        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 응답을 JSON으로 변환하는 중 오류 발생");
        }

        return oAuthToken;
    }

    //Token으로 정보 요청

}