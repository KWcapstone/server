package com.kwcapstone.Token;

import com.kwcapstone.Domain.Entity.MemberRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

//Refresh Token 넣고 다시 accessToken 하고 refreshToken 발급받을 때 필요한 함수들
@Component
public class JwtTokenProvider {

    //필요한 필드
    //Http request의 Authorization 헤더에서 JWT 추출하기 위함
    private static final String  HEADER_STRING
            = "Authorization";
    //"Bearer" 로 시작하는 Jwt 토큰 처리 위한 값
    private static final String HEADER_STRING_PREFIX = "Bearer ";

    private final SecretKey secretKey;//암호화키 -> 토큰이 변조되지 않았음을 검증
    private final long aTValidityMilliseconds; //AccessToken 유효기간
    private final long rTValidityMilliseconds; //RefrshToken 유효기간

    //생성자
    public JwtTokenProvider(
            @Value("${jwt.secret-key}") final String secretKey,
            @Value("${jwt.access-token-validity}") final long aTValidityMilliseconds,
            @Value("${jwt.refresh-token-validity}") final long rTValidityMilliseconds
    ){
        //UTF-8 인코딩을 사용해서 문자열을 인코딩하고 HMAC-SHA 알고리즘으로 Key객체를 생성하는거임
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.aTValidityMilliseconds = aTValidityMilliseconds;
        this.rTValidityMilliseconds = rTValidityMilliseconds;
    }

    //token 추출
    public String extractToken(final HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HEADER_STRING);

        if (authorizationHeader != null &&
                authorizationHeader.startsWith(HEADER_STRING_PREFIX)) {
            //고침
            String token = authorizationHeader.substring(7).trim();
            return token;
        }
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Auth_001 : refresh 토큰 추출에 실패하였습니다.");
    }

    //Token 생성
    //1. 소셜 로그인 발급
    public String createAccessToken(ObjectId memberId, String role){
        return createToken(memberId, role, aTValidityMilliseconds);
    }

    public String createRefreshToken(ObjectId memberId, String role){
            return createToken(memberId, role,rTValidityMilliseconds);
    }

    private String ConvertToStringType(ObjectId memberId){
        return memberId.toHexString();
    }

    //소셜 로그인 & 일반 로그인 jwt 발급
    private String createToken(ObjectId memberId, String role, Long validityMilliseconds){
        String stringMemberId = ConvertToStringType(memberId);
        //Jwt에 사용자 정보를 저장하기 위해 필요한 것
        Claims claims = Jwts.claims();
        claims.put("memberId", stringMemberId);
        claims.put("role", role);

        //현재시간 가져오기
        ZonedDateTime now = ZonedDateTime.now();
        //현재시간을 통해 jwt 발급 시점을 기록하기
        ZonedDateTime tokenValidity
                = now.plus(Duration.ofMillis((validityMilliseconds)));

        return Jwts.builder()
                .setClaims(claims) //사용자 정보 설정
                .setIssuedAt(Date.from(now.toInstant())) //발급 시간(iat) 설정
                .setExpiration(Date.from(tokenValidity.toInstant())) //만료 시간 설정
                .signWith(secretKey, SignatureAlgorithm.HS256) //서명 추가
                .compact(); //최종 Jwt 생성
    }

    //Jwt 에서 사용자 Id 추출(ObjecctId 타입인데 String 타입으로 추출)
    public String getId(String token) {
        return getClaims(token).getBody().get("memberId", String.class);
    }

    //Jwt 에서 role 추출
    public String getRole(String token) {
        return getClaims(token).getBody().get("role", String.class);
    }

    //토큰 확인
    public boolean isTokenValid(String token) {
        try {
            //Jwt의 사용자 정보 가져오기
            Jws<Claims> claims = getClaims(token);
            //만료 시간 가져오기
            Date expiredDate = claims.getBody().getExpiration();
            //현재 날짜 가져오기
            Date now = new Date();

            //만료 시간이 현재 이후인지 아닌지 확인하는 것(이후면 true)
            return expiredDate.after(now);
        } catch (ExpiredJwtException e) {
            //jwt 만료되었을 때 발생하는 예외
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_002");
        } catch (SecurityException e) {
            //Jwt의 서명이 손상되었거나 위변조 된 경우
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_003 : 토큰의 서명이 손상되었거나 위변조 되었습니다.");
        } catch (MalformedJwtException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AUTH_004 : Jwt 형식이 올바르지 않습니다.");
        } catch (UnsupportedJwtException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_005 : 지원되지 않은 JWT 형식입니다.");
        } catch (IllegalArgumentException e){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_006 : Jwt가 null이거나 공백 문자열인 경우");
        }
    }

    //TOKEN으로 JWT에서 사용자 정보 가져오기
    private Jws<Claims> getClaims(String token) {
        return Jwts.parserBuilder() //파서 객체 생성
                .setSigningKey(secretKey) //서명 검증을 위한 secretKey 설정
                .build().parseClaimsJws(token); //Jwt 파싱해서 사용자 정보 추출 및 검증 수행
    }
}
