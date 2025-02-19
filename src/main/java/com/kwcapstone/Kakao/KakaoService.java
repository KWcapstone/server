package com.kwcapstone.Kakao;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    }
}
