package com.kwcapstone.Kakao;

import com.kwcapstone.Repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional //동시성과 DB를 위함
public class TokenService {
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    //refreshToken 업데이트
    public TokenResponse reissueToken(HttpServletRequest request) {
        //token 추출
        String refreshToken = jwtTokenProvider.extractToken(request);
        //refreshToken이랑 같은 Token 정보가 있는지 확인
        Token token = getToken(refreshToken);

        Long socialId = validateRefreshToken(refreshToken);
        String newAccessToken
                = jwtTokenProvider.createAccessToken(memberId);
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(memberId);

        // refreshToken 업데이트
        token.changeToken(newRefreshToken);
        return AuthConverter.toTokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    private Token getToken(String refreshToken) {
        Optional<Token> token = tokenRepository.findByRefreshToken(refreshToken);
        if (!token.isPresent()) {
            throw new AuthException(NOT_CONTAIN_TOKEN);
            // Logout 되어있는 상황
        }
        return token.get();
    }

    //socialId->String
    private Long validateRefreshToken(String refreshToken) {
        jwtTokenProvider.isTokenValid(refreshToken);
        Long socialId = jwtTokenProvider.getId(refreshToken);
        return socialId;
    }
}
