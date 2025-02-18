package com.kwcapstone.Kakao;

import com.kwcapstone.Repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                = jwtTokenProvider.createAccessToken(socialId);
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(socialId);

        // refreshToken 업데이트
        token.changeRefreshToken(newRefreshToken);
        return TokenConvert
                .toTokenRefreshResponse(newAccessToken,newRefreshToken);
    }


    private Boolean getToken(String refreshToken) {
        Optional<Token> token
                = tokenRepository.findByRefreshToken(refreshToken);

        if (!token.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "해당하는 refreshToken이 저장되어있지 않습니다.");
        }

        return token.get();
    }

    //socialId->String
    private Long validateRefreshToken(String refreshToken) {
        //토큰이 존재하는가?
        jwtTokenProvider.isTokenValid(refreshToken);
        //jwt에서 정보 빼오는거라서 Long 괜추나
        Long socialId = jwtTokenProvider.getId(refreshToken);
        return socialId;
    }
}
