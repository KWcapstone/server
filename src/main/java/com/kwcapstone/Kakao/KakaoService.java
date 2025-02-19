package com.kwcapstone.Kakao;

import com.kwcapstone.Domain.Entity.Member;
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
        OAuthToken oAuthToken = kaKaoProvider.requestToken(code);

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

        //존재하면 새로운 accesstoekn하고 refresh token만 다시 주는 걸로
        if(queryMember.isPresent()){
            Member member = queryMember.get();

        }
    }

    //기존 유저
    private KakaoResponse.KakaoLoginResponse getKakaoResponseForPresentUser(Member member){
        //accessToken  새로 만들기
        String newAccessToken
                = jwtTokenProvider.createAccessToken(member.getSocialId(),"kakao");

        //refreshToken 새로 만들기
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(member.getSocialId(),"kakao");

        //db에 token 저장하기
        Optional<Token> isPresent = tokenRepository.findByMemberId(member.getMemberId());
    }
}
