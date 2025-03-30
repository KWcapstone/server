package com.kwcapstone.Token.Service;

import com.kwcapstone.Kakao.Service.KaKaoProvider;
import com.kwcapstone.Naver.Service.NaverProvider;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Service.GoogleOAuthService;
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
    private final KaKaoProvider kaKaoProvider;
    private final NaverProvider naverProvider;
    private final GoogleOAuthService googleOAuthService;
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

        System.out.println(role);
        ObjectId memberId = ConvertToObjectId(stringMemberId);

        //SocialAccesstoken이 만료되엇는지 확인하기
        if(isSocialRole(role)){
            String socialAccessToken = token.getSocialAccessToken();

            if(isSocialAccessTokenExpired(socialAccessToken, role)){
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "소셜 accessToken이 만료되었습니다. 다시 로그인 해주세요."
                );

            }
        }

        String newAccessToken
                = jwtTokenProvider.createAccessToken(memberId, role);
        String newRefreshToken
                = jwtTokenProvider.createRefreshToken(memberId, role);

        // refreshToken 업데이트
        token.changeToken(newAccessToken, newRefreshToken,token.getSocialAccessToken());
        tokenRepository.save(token);
        return TokenConvert
                .toTokenRefreshResponse(newAccessToken,newRefreshToken);
    }

    //role 확인하기
    private  boolean isSocialRole(String role){
        return role.equals("KAKAO") ||
                role.equals("NAVER") ||
                role.equals("GOOGLE");
    }

    //socialAccesstoken이 만료되었는지 확인하기
    private boolean isSocialAccessTokenExpired(String accessToken, String role){
        try{
            switch(role){
                case "KAKAO":
                    return !kaKaoProvider.validateAccessToken(accessToken); // 만료된 것이 true로 나오게 하기
                case "NAVER" :
                    return !naverProvider.validateAccessToken(accessToken);
                case "GOOGLE" :
                    return !googleOAuthService.validateAccesstoken(accessToken);
                default:
                    return false;
            }
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "isSocialAccesstoken 만료에서 오류가 발생했습니다." + e.getMessage());
        }
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
