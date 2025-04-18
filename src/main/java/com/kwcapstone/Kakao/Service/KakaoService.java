package com.kwcapstone.Kakao.Service;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.Kakao.Dto.KaKaoProfile;
import com.kwcapstone.Kakao.Dto.KakaoResponse;
import com.kwcapstone.Token.Domain.Dto.OAuthToken;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
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
        OAuthToken oAuthToken = getTokenRequest(code);
        String socialAccessToken = oAuthToken.getAccess_token();

        KaKaoProfile kaKaoProfile;
        //kakao user 정보 가져오기
        try{
            kaKaoProfile
                    = kaKaoProvider.getProfile(socialAccessToken);
        }catch (Exception e){
            throw new RuntimeException("카카오 정보 가져오는 과정에서 오류 발생했습니다. "
                    + e.getMessage());
        }

        //유저 정보 있는지 확인
        Optional<Member> queryMember =
                memberRepository.findByEmail(
                        kaKaoProfile.getKakaoAccount().getEmail());


        KakaoResponse.KakaoLoginResponse tokenResponse
                = new KakaoResponse.KakaoLoginResponse();

        //존재하면 새로운 access token하고 refresh token만 다시 주는 걸로
        if(queryMember.isPresent()){
            tokenResponse = getKakaoResponseForPresentUser(queryMember.get(), socialAccessToken);
            return new KakaoResponse.KakaoLoginResponse(queryMember.get().getMemberId(),
                    tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
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

        return getKakaoResponseForNewUser(member,socialAccessToken);
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
    private KakaoResponse.KakaoLoginResponse getKakaoResponseForPresentUser(Member member, String socialAccessToken){
        //accessToken  새로 만들기
        String newAccessToken
                = jwtTokenProvider.createAccessToken(member.getMemberId(), member.getRole().getTitle());

        //refreshToken 새로 만들기
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(member.getMemberId(),member.getRole().getTitle());

        //db에 token 저장하기
        Optional<Token> isPresent
                = tokenRepository.findByMemberId(member.getMemberId());

        if(isPresent.isPresent()){
            Token token = isPresent.get();
            token.changeToken(newAccessToken,newRefreshToken, socialAccessToken);
            System.out.println("저장된 refreshToken: " + newRefreshToken);
            tokenRepository.save(token);
        }else{
            tokenRepository.save(
                    new Token(newAccessToken, newRefreshToken, member.getMemberId(), socialAccessToken));
        }

        return new KakaoResponse.KakaoLoginResponse(member.getMemberId(), newAccessToken, newRefreshToken);
    }

    //새로운 유저
    private KakaoResponse.KakaoLoginResponse getKakaoResponseForNewUser(Member member, String socialAccessToken){
        //member 새로 저장(save)를 해야 저장된다고 함.
        Member savedMember = memberRepository.save(member);

        //accessToken  새로 만들기
        String newAccessToken
                = jwtTokenProvider.createAccessToken(member.getMemberId(), "kakao");

        //refreshToken 새로 만들기
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(member.getMemberId(),"kakao");


        tokenRepository.save(new Token(newAccessToken, newRefreshToken, member.getMemberId(), socialAccessToken));

        return new KakaoResponse.KakaoLoginResponse(member.getMemberId(),newAccessToken, newRefreshToken);
    }
}
