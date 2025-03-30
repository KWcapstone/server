package com.kwcapstone.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.GoogleLogin.Auth.GoogleUser;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;

import org.springframework.http.*;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@PropertySource("classpath:application.properties")
public class GoogleOAuthService {
    private final RestTemplate restTemplate;
    private final TokenRepository tokenRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public GoogleOAuthService(
            TokenRepository tokenRepository,
            MemberRepository memberRepository,
            JwtTokenProvider jwtTokenProvider // 생성자로 주입
    ) {
        this.restTemplate = new RestTemplate();
        this.tokenRepository = tokenRepository;
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // JWT Access 토큰과 Refresh 토큰을 받기 위한 부분
    public BaseResponse<String> getAccessToken(String authorizationCode) {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        String finalCode = authorizationCode;
        if (authorizationCode.contains("%")) {  // 인코딩 되어있으면 디코딩
            finalCode = URLDecoder.decode(authorizationCode, StandardCharsets.UTF_8);
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", finalCode);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request
                = new HttpEntity<>(params, headers);


        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenEndpoint, HttpMethod.POST, request,String.class);

            Map<String, Object> responseMap;
            try {
                responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Google OAuth 응답 JSON 파싱 실패: " + e.getMessage());
            }

            String accessToken = (String) responseMap.get("access_token");
            String refreshToken = (String) responseMap.get("refresh_token");

            Token token = new Token(accessToken, refreshToken, null);

            // 여기서 왜 token을 주는게 아니고 accessToken을 주지....???????
            return new BaseResponse<>(HttpStatus.OK.value(),"Google Access Token 발급 성공", accessToken);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return new BaseResponse<>(e.getStatusCode().value(),
                    "Google Oauth 요청 중 오류 발생" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Google OAuth 요청 실패: " + e.getMessage());
        }
    }

    public BaseResponse<GoogleUser> getUserInfo(String accessToken) {
        String userInfoEndpoint = "https://www.googleapis.com/oauth2/v3/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUser> response = restTemplate.exchange(
                    userInfoEndpoint, HttpMethod.GET, request, GoogleUser.class);
            return new BaseResponse<>(HttpStatus.OK.value(), "Google 사용자 정보 조회 성공", response.getBody());
        } catch (HttpStatusCodeException e) {
            return new BaseResponse<>(e.getStatusCode().value(),
                    "Google 사용자 정보 요청 실패: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Google 사용자 정보 요청 실패: " + e.getMessage());
        }
    }
}
