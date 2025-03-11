package com.kwcapstone.Naver.Service;

import com.kwcapstone.Naver.Dto.NaverResponse;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Dto.OAuthToken;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NaverService {
    private final TokenRepository tokenRepository; //토큰 저장을 위함
    private final NaverProvider naverProvider; //네이버 요청을 위함
    private final JwtTokenProvider jwtTokenProvider;//jwt 토큰 발급을 위함
    private final MemberRepository memberRepository; //member 저장하기 위함

    //naver Login
    //MemberId랑 accessToken 보내주기
    @Transactional
    public NaverResponse.NaverLoginResponse naverLogin(String code){
        //naver token 발급
        OAuthToken oAuthToken =
    }

    //토큰 발급
    private OAuthToken getTokenRequest(String code){
        OAuthToken oAuthToken;
        try{
            oAuthToken = naverProvider.requestToken(code);
        }catch (HttpClientErrorException e){
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "네이버 토큰 생성에서 잘못된 요청입니다.");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "인증이 필요합니다.(clientId, clientsecretKey가 잘못됐을 가능성 있습니다.)");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "토큰을 가져오는 과정에서 서버 오류가 발생했습니다." +e.getMessage());

        } catch (HttpServerErrorException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "네이버 서버 오류");
        } catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "기타 오류 발생했습니다."+ e.getMessage());
        }

        return oAuthToken;
    }
}
