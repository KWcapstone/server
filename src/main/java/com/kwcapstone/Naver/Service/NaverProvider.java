package com.kwcapstone.Naver.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Naver.Dto.NaverProfile;
import com.kwcapstone.Token.Domain.Dto.OAuthToken;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.Repository.TokenRepository;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class NaverProvider {
    private final TokenRepository tokenRepository;
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
                    "code 값이 null 또는 빈칸(공백)입니다.");
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

        //요청할 객체 생성
        HttpEntity<MultiValueMap<String,String>> naverTokenRequest
                =new HttpEntity<>(params, headers);

        //네이버 OAuth 토큰 요청하기
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    "https://nid.naver.com/oauth2.0/token", HttpMethod.POST,
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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "네이버 응답을 JSON으로 변환하는 중 오류 발생");
        }

        return oAuthToken;
    }

    //Token으로 정보 요청
    public NaverProfile getProfile(String token){
        //token이 없거나 빈칸, 공백일 경우의 예외처리
        if(token == null || token.trim().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토큰 값이 null 또는 빈칸(공백)입니다.");
        }
        //Http 요청을 위해
        RestTemplate restTemplate = new RestTemplate();

        //Http 요청 담기 위해
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        //네이버는 넘길 정보가 없기 때문에 MultiValue 뛰어넘기

        //Http 요청하기
        HttpEntity<Void> naverProfileRequest
                = new HttpEntity<>(headers);

        //정보 받아오기
        ResponseEntity<String> response;

        //예외처리
        try{
            response = restTemplate.exchange(
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET,
                    naverProfileRequest, String.class);
        }catch (RestClientException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "네이버 프로필 서버가 응답하지 않습니다.");
        }

        //객체 매핑 생성
        ObjectMapper objectMapper = new ObjectMapper();
        NaverProfile naverProfile = null;

        //예외처리
        try{
            naverProfile = objectMapper.readValue(
                    response.getBody(),NaverProfile.class);
        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "네이버 유저 정보 불러오는 것에 실패했습니다.(서버 오류)");
        }
        return naverProfile;
    }


    //네이버 연동 해체
    public boolean naverUnLink(Member member) {
        RestTemplate restTemplate = new RestTemplate();
        Optional<Token> token = tokenRepository.findByMemberId(member.getMemberId());
        if(!token.isPresent()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "네이버 연동 해체 과정에서 토큰이 존재하지 않는 오류가 발생했습니다.");
        }

        String accessToken = token.get().getAccessToken();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client", clientId);
        params.add("client_secret", clientSecret);
        params.add("access_token",accessToken);
        params.add("grant_type", "authorization_code");


        try{
            URI uri = new URI("https://nid.naver.com/oauth2.0/token" + params);

            ResponseEntity<String> response;
            response = restTemplate.exchange(uri,
                    HttpMethod.GET, null, String.class);

            if(response == null){
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "네이버 연동 해체 응답을 받지 못했습니다.");
            }
            else{
                return true;
            }
        }catch (RestClientException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "네이버 서버가 응답하지 않습니다.");
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "네이버 연동 해체 요청 URI 가 잘못된 형식입니다.");
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "네이버 연동 해체 중 예기치 못한 오류가 발생했습니다.");
        }
    }

}