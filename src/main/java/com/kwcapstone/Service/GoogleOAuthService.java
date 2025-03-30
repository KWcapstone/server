package com.kwcapstone.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Domain.Entity.Member;
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
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    public String getAccessToken(String authorizationCode) {
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
                //return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Google OAuth 응답 JSON 파싱 실패: " + e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google OAuth 응답JSON 파싱 실패: " + e.getMessage());
            }

            String accessToken = (String) responseMap.get("access_token");
            String refreshToken = (String) responseMap.get("refresh_token");

            //Token token = new Token(accessToken, refreshToken, null);

            // 여기서 왜 token을 주는게 아니고 accessToken을 주지....???????
            //return BaseResponse<>(HttpStatus.OK.value(),"Google Access new Token 발급 성공", accessToken);
            return accessToken;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            //return new BaseResponse<>(e.getStatusCode().value(), "Google Oauth 요청 중 오류 발생" + e.getResponseBodyAsString());
            throw new ResponseStatusException(e.getStatusCode(), "Google Oauth 요청 중 오류 발생" + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            //return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Google OAuth 요청 실패: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google OAuth 요청 실패: " + e.getMessage());
        }
    }


    //BaseResponse<GoogleUser>
    public GoogleUser getUserInfo(String accessToken) {
        String userInfoEndpoint = "https://www.googleapis.com/oauth2/v3/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUser> response = restTemplate.exchange(
                    userInfoEndpoint, HttpMethod.GET, request, GoogleUser.class);
            //return new BaseResponse<>(HttpStatus.OK.value(), "Google 사용자 정보 조회 성공", response.getBody());
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            //return new BaseResponse<>(e.getStatusCode().value(), "Google 사용자 정보 요청 실패: " + e.getResponseBodyAsString());
            throw new ResponseStatusException(e.getStatusCode(), "Google 사용자 정보 요청 실패: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            //return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Google 사용자 정보 요청 실패: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google 사용자 정보 요청 실패: " + e.getMessage());
        }
    }

    //구글 연동 해체
    public boolean googleUnLink(Member member) {
        RestTemplate restTemplate = new RestTemplate();
        Optional<Token> token = tokenRepository.findByMemberId(member.getMemberId());
        if(!token.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "구글 연동 해체 과정에서 토큰이 존재하지 않는 오류가 발생했습니다.");
        }
        String accessToken = token.get().getAccessToken();

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 본문 설정
        String requestBody = "token=" + accessToken;
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/revoke", HttpMethod.POST,
                    requestEntity, String.class);

            if(response == null){
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"구글 연동 해체 응답을 받지 못했습니다.");
            }else {
                return true;
            }
        }catch(RestClientException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "구글 서버가 응답하지 않습니다.");
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "구글 연동 해체 중 예기치 못한 오류가 발생했습니다.");
        }

    }

    //isValidAccessToken
    public boolean validateAccesstoken(String accessToken) {
        String uri = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;

        try{
            ResponseEntity<String> response = new RestTemplate().getForEntity(uri, String.class);
            if(response == null){
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "구글 socialAccessToken 유효성을 확인하지 못했습니다.");
            }
            return response.getStatusCode().is2xxSuccessful();
        }catch (RestClientException e){
            return false;
        }
    }
}
