package com.kwcapstone.Service;

import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Exception.BaseException;
import com.kwcapstone.GoogleLogin.Auth.GoogleUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;

import org.springframework.http.*;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

@Service
@PropertySource("classpath:application.properties")
public class GoogleOAuthService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    public BaseResponse<String> getAccessToken(String authorizationCode) {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", authorizationCode);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenEndpoint, HttpMethod.POST, request,
                    new ParameterizedTypeReference<>() {});
            return new BaseResponse<>(HttpStatus.OK.value(),
                    "Google Access Token 발급 성공", (String) response.getBody().get("access_token"));
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
