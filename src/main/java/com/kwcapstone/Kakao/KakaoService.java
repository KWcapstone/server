package com.kwcapstone.Kakao;

import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public
}
