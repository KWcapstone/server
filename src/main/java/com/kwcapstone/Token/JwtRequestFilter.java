package com.kwcapstone.Token;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
//jwt 한 번만 filter 검사하기 위해 하는 상속
//이거 안하면 jwt 인증이 여러번 나고 그러면 성능 저하, 중복 인증 문제 등이 발생
public class JwtRequestFilter{
    private final JwtTokenProvider jwtTokenProvider;

    private boolean isPermitAllPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.equals("/swagger-ui.html")
                // open-api
                || path.startsWith("/api/open-api")

                // eb health check 용 API
                || path.equals("/health")

                // 로그인/토큰발급 등등
                || path.equals("/auth/sign_up")
                || path.equals("/auth/email_duplication")
                || path.equals("/auth/email_verification")
                || path.equals("/auth/login")
                || path.equals("/login/oauth2/code/google") //추후 개선 가능성 있음
                || path.equals("/auth/agree")
                || path.equals("/auth/find_id")
                || path.equals("/auth/find_pw")
                || path.equals("/auth/change_pw")
                || path.equals("/auth/refresh")
                || path.startsWith("/auth/login")
                || path.startsWith("/test")

                // 필요하다면 다른 permitAll 경로들도 추가
                // ...
                ;
    }
}
