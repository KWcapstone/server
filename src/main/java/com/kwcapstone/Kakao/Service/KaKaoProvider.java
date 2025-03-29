package com.kwcapstone.Kakao.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Kakao.Dto.KaKaoProfile;
import com.kwcapstone.Token.Domain.Dto.OAuthToken;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KaKaoProvider {
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
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
        //code가 null 이거나 빈 공백인 경우의 예외처리
        if(code == null | code.trim().isEmpty()){
            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "code 값이 null 또는 빈칸(공백)입니다.");
        }

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
        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(
                    "https://kauth.kakao.com/oauth/token", HttpMethod.POST, kakaoTokenRequest, String.class);

        }catch (RestClientException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 서버가 응답하지 않습니다.");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        OAuthToken oauthToken = null;

        try{
            oauthToken = objectMapper.readValue(
                    response.getBody(),OAuthToken.class);
        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "카카오 정보 불러오기에 실패하였습니다.");
        }
        return oauthToken;
    }

    //Token으로 정보 요청
    public KaKaoProfile getProfile(String token){
        if(token == null || token.trim().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "토큰 값이 null 또는 빈칸(공백)입니다.");
        }
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
        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.POST,
                    kakaoProfileRequest, String.class);
        }catch (RestClientException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 프로필 서버가 응답하지 않습니다.");
        }

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

    //카카오 연동 해체
    @Transactional
    public boolean kakaoUnLink(Member member){
        RestTemplate restTemplate = new RestTemplate();

        Optional<Token> token = tokenRepository.findByMemberId(member.getMemberId());
        if(!token.isPresent()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 연동 해체 과정에서 토큰이 존재하지 않는 오류가 발생했습니다.");
        }

        String accessToken = token.get().getSocialAccessToken();
        try{
            jwtTokenProvider.isTokenValid(accessToken);
        }catch(ResponseStatusException e){
            if(e.getStatusCode() == HttpStatus.UNAUTHORIZED && e.getReason().contains("토큰이 만료되었습니다.")){
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"AccessToken 이 만료되었습니다.");
            }
            throw e;
        }
        String socialId = member.getSocialId();

        //System.out.print("socialId"+ socialId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        headers.add("Accept", "application/json");

        System.out.println("📡 요청 헤더: " + headers.toString());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", socialId);

        HttpEntity<MultiValueMap<String, String>> unLinkRequest
                = new HttpEntity<>(params, headers);

        ResponseEntity<String> response;

        try{
            response = restTemplate.exchange(
                    "https://kapi.kakao.com/v1/user/unlink", HttpMethod.POST,
                    unLinkRequest, String.class);
            System.out.println("📥 응답 코드: " + response.getStatusCode());
            System.out.println("📄 응답 바디: " + response.getBody());

            if(response == null){
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 연동 해체 응답을 받지 못했습니다.");
            }else{
                System.out.println("socialId: " + socialId + " response: " + response.getBody());
                return true;
            }
        }catch (HttpStatusCodeException e){
            // 여기서 실제 응답 확인 가능
            System.out.println("❗카카오 응답 코드: " + e.getStatusCode());
            System.out.println("❗카카오 응답 바디: " + e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 서버가 응답하지 않습니다.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 연동 해체 중 예기치 못한 오류가 발생했습니다.");
        }
    }

    //isValidAccessToken
    public boolean validateAccessToken(String accessToken){
        String uri = "https://kapi.kakao.com/v1/user/access_token_info";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = new RestTemplate().exchange(
                    uri, HttpMethod.GET, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        }catch(RestClientException e){
            return false;
        }
    }
}
