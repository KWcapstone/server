package com.kwcapstone.Naver.Service;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.Kakao.Service.KaKaoProvider;
import com.kwcapstone.Naver.Dto.NaverProfile;
import com.kwcapstone.Naver.Dto.NaverResponse;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Dto.OAuthToken;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NaverService {
    private final TokenRepository tokenRepository; //토큰 저장을 위함
    private final NaverProvider naverProvider; //네이버 요청을 위함
    private final JwtTokenProvider jwtTokenProvider;//jwt 토큰 발급을 위함
    private final MemberRepository memberRepository; //member 저장하기 위함
    private final KaKaoProvider kaKaoProvider;

    //naver Login
    //MemberId랑 accessToken 보내주기
    @Transactional
    public NaverResponse.NaverLoginResponse naverLogin(String code){
        //naver token 발급
        OAuthToken oAuthToken = getTokenRequest(code);

        //네이버 프로필 정보 불러오기
        NaverProfile naverProfile;

        try{
            naverProfile
                    = naverProvider.getProfile(oAuthToken.getAccess_token());
        }catch (HttpClientErrorException e){
            if(e.getStatusCode() == HttpStatus.BAD_REQUEST){
                throw new ResponseStatusException
                        (HttpStatus.BAD_REQUEST, "네이버 프로필 가져오던 중 잘못된 요청입니다.");
            }
            else if(e.getStatusCode() == HttpStatus.UNAUTHORIZED){
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "네이버 인증이 필요합니다.(ClientId, ClientSecretKey가 잘못되었습니다.)");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 프로필 가져오던 중 HttpClientException 발생" + e.getMessage());
        }catch (HttpServerErrorException e){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "네이버 서버 오류 발생했습니다.");
        }catch(Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 프로필 가져오던 중 기타 오류 발생했습니다." + e.getMessage());
        }

        //유저 정보 있는지 확인
        Optional<Member> queryMember=
                memberRepository.findByEmail(
                        naverProfile.getResponse().getEmail());

        //NaverResponse 에 대한 토큰
        NaverResponse.NaverLoginResponse tokenResponse
                = new NaverResponse.NaverLoginResponse();

        //존재하면 새로운 accessToken + refresh만 발급
        if(queryMember.isPresent()){
            tokenResponse = getNaverResponseForUser(queryMember.get());
            return new NaverResponse.NaverLoginResponse(queryMember.get().getMemberId(),
                    tokenResponse.getAccessToken());
        }

        //존재하지 않음
        //약관 동의 생기면 변경해야 함
        Member member = Member.builder()
                .name(naverProfile.getResponse().getNickname())
                .email(naverProfile.getResponse().getEmail())
                .agreement(true)
                .image(naverProfile.getResponse().getProfileImage())
                .socialId(naverProfile.getResponse().getId())
                .role(MemberRole.NAVER)
                .build();

        return get
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

    //기존 유저
    private NaverResponse.NaverLoginResponse getNaverResponseForUser(Member member){
        //accessToken 새로 만들기
        String newAccessToken
                = jwtTokenProvider.createAccessToken(member.getMemberId(), member.getRole().getTitle());

        //refreshToken 새로 만드기
        String newrefreshToken
                = jwtTokenProvider.createRefreshToken(member.getMemberId(), member.getRole().getTitle());

        //db에 token 저장하기
        Optional<Token> tokenIsPresent
                = tokenRepository.findByMemberId(member.getMemberId());

        if(tokenIsPresent.isPresent()){
            tokenIsPresent.get().changeToken(newAccessToken, newrefreshToken);
        }else{
            tokenRepository.save(
                    new Token(newAccessToken, newrefreshToken, member.getMemberId()));
        }
        return new NaverResponse.NaverLoginResponse(member.getMemberId(), newAccessToken);
    }

    //새로운 유저
    private NaverResponse.NaverLoginResponse getNaverResponseForNewUser(Member member){
        //member 새로 저장
        Member savedMember = memberRepository.save(member);

        //accessToken 새로 만들기
        String newAccessToken
                = jwtTokenProvider.createAccessToken(member.getMemberId(), MemberRole.NAVER.getTitle());

        //refreshToken 새로 만들기
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(member.getMemberId(), MemberRole.NAVER.getTitle());

        //token 저장
        tokenRepository.save(new Token(newAccessToken, newRefreshToken, member.getMemberId()));

        return new NaverResponse.NaverLoginResponse(member.getMemberId(), newAccessToken);
    }
}
