package com.kwcapstone.Token.Service;

import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.Domain.Dto.TokenResponse;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Domain.Convert.TokenConvert;
import com.kwcapstone.Token.Repository.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional //동시성과 DB를 위함
public class TokenService {
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    //String -> obejctId
    public ObjectId ConvertToObjectId(String memberId){
        return new ObjectId(memberId);
    }

    //refreshToken 업데이트
    public TokenResponse reissueToken(String refreshToken) {
        //token 추출
        //String refreshToken = jwtTokenProvider.extractToken(request).trim();
        //System.out.println("클라이언트가 보낸 refereshToken: {}"+ refreshToken);
        //refreshToken이랑 같은 Token 정보가 있는지 확인

        Token token = getToken(refreshToken);

        //memberId
        String stringMemberId = validateRefreshToken(refreshToken);
        String role = findRoleByRefreshToken(refreshToken);

        ObjectId memberId = ConvertToObjectId(stringMemberId);

        String newAccessToken
                = jwtTokenProvider.createAccessToken(memberId, role);
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(memberId, role);

        // refreshToken 업데이트
        token.changeToken(newAccessToken, newRefreshToken);
        tokenRepository.save(token);
        return TokenConvert
                .toTokenRefreshResponse(newAccessToken,newRefreshToken);
    }


    private Token getToken(String refreshToken) {
        Optional<Token> token
                = tokenRepository.findByRefreshToken(refreshToken);
        System.out.println("getToken에서의 "+refreshToken);
        System.out.println("DB에 저장된 refreshToken들:");

        tokenRepository.findAll().forEach(t -> {
            System.out.println("-> " + t.getRefreshToken());
        });

        if (!token.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "해당하는 refreshToken이 저장되어있지 않습니다.");
        }

        return token.get();
    }

    //ObjectId
    private String validateRefreshToken(String refreshToken) {
        //토큰이 존재하는가?
        jwtTokenProvider.isTokenValid(refreshToken);

        String memberId = jwtTokenProvider.getId(refreshToken);

        return memberId;
    }

    //role
    private String findRoleByRefreshToken(String refreshToken) {
        String role = jwtTokenProvider.getRole(refreshToken);
        return role;
    }
}
