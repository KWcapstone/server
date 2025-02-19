package com.kwcapstone.Kakao;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.OptionalLong;

@Service
@RequiredArgsConstructor
public class KakaoService {
    private final TokenRepository tokenRepository;
    private final KaKaoProvider kaKaoProvider;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    //kakao Login
    //memberId랑 accesstoken이랑 refreshtoken 보내주기
    @Transactional
    public KakaoResponse.KakaoLoginResponse kakaoLogin(String code){
        //kakao token 발급
        OAuthToken oAuthToken = getTokenRequest(code);

        KaKaoProfile kaKaoProfile;
        //kakao user 정보 가져오기
        try{
            kaKaoProfile
                    = kaKaoProvider.getProfile(oAuthToken.getAccess_token());
        }catch (Exception e){
            throw new RuntimeException("카카오 정보 가져오는 과정에서 오류 발생했습니다. "
                    + e.getMessage());
        }

        //유저 정보 있는지 확인
        Optional<Member> queryMember =
                memberRepository.findByEmail(
                        kaKaoProfile.getKakaoAccount().getEmail());

        KakaoResponse.KakaoTokenResponse tokenResponse
                = new KakaoResponse.KakaoTokenResponse();

        //존재하면 새로운 accesstoekn하고 refresh token만 다시 주는 걸로
        if(queryMember.isPresent()){
            tokenResponse = getKakaoResponseForPresentUser(queryMember.get());
            return new KakaoResponse.KakaoLoginResponse(queryMember.get().getMemberId(),
                    tokenResponse.getAccessToken(),tokenResponse.getRefreshToken());
        }

        //존재하지 않음
        Member member = Member.builder()
                .name(kaKaoProfile.getKakaoAccount()
                        .getProfile().getNickname())
                .email(kaKaoProfile.getKakaoAccount().getEmail())
                .agreement(true)
                .image(kaKaoProfile.getKakaoAccount().getProfile().getProfileImageUrl())
                .socialId(String.valueOf(kaKaoProfile.getId()))
                .role(MemberRole.KAKAO)
                .build();

        return getKakaoResponseForNewUser(member);
    }

    //token 발급
    private OAuthToken getTokenRequest(String code){
        OAuthToken oAuthToken;
        try{
            oAuthToken = kaKaoProvider.requestToken(code);
        }catch (Exception e){
            throw new RuntimeException("코드 번호가 유효하지 않습니다." + e.getMessage());
        }

        return oAuthToken;
    }
    //기존 유저
    private KakaoResponse.KakaoTokenResponse getKakaoResponseForPresentUser(Member member){
        //accessToken  새로 만들기
        String newAccessToken
                = jwtTokenProvider.createAccessToken(member.getSocialId(),"kakao");

        //refreshToken 새로 만들기
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(member.getSocialId(),"kakao");

        //db에 token 저장하기
        Optional<Token> isPresent
                = tokenRepository.findByMemberId(member.getMemberId());

        if(isPresent.isPresent()){
            isPresent.get().changeToken(newAccessToken,newRefreshToken);
        }else{
            tokenRepository.save(
                    new Token(newAccessToken, newRefreshToken, member.getMemberId()));
        }

        return new KakaoResponse.KakaoTokenResponse(newAccessToken, newRefreshToken);
    }

    //새로운 유저
    private KakaoResponse.KakaoLoginResponse getKakaoResponseForNewUser(Member member){
        //accessToken  새로 만들기
        String newAccessToken
                = jwtTokenProvider.createAccessToken(member.getSocialId(),"kakao");

        //refreshToken 새로 만들기
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(member.getSocialId(),"kakao");

        tokenRepository.save(new Token(newAccessToken, newRefreshToken, member.getMemberId()));
        memberRepository.save(member);

        return new KakaoResponse.KakaoLoginResponse(member.getMemberId(),newAccessToken,newRefreshToken);
    }
}
