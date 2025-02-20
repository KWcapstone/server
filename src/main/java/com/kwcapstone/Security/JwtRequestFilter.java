package com.kwcapstone.Security;

import com.kwcapstone.Token.JwtTokenProvider;
import jakarta.security.auth.message.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
//jwt 한 번만 filter 검사하기 위해 하는 상속
//이거 안하면 jwt 인증이 여러번 나고 그러면 성능 저하, 중복 인증 문제 등이 발생
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    //jwt 기반 인증 처리 필터
    //(HTTP 요청 들어올 때 jwt를 검증하고 인증정보를 저장하는 역할)
    //service에서 안하는 이유 : 비효율적, 보안적으로 좋지 않음
    //얘는 호출된 곳이 없어도 자동으로 호출하기 때문에 잇어야함
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        //특정 리소스를 식별하는 문자열(url은 위치를 나타내고 도메인을 제외한 경로만을 얻기위함)
        //인증이 필요 없는 경로를 확인하기 위함
        String path = request.getRequestURI();

        //Swagger, 로그인 등 "인증이 필요 없는 경로" 검사 -> jwt 검증 스킵
        if (isPermitAllPath(path)) {
            filterChain.doFilter(request, response);
            return;}

        //jwt 검증이 필요한 경우
        try {
            //jwt 추출
            String token = jwtTokenProvider.extractToken(request);

            //token이 유효한지, 만료되지 않았는지 checking
            if (jwtTokenProvider.isTokenValid(token)) {
                //jwt에서 사용자 id를 추출
                String socialId = jwtTokenProvider.getId(token);

                //인증을 처리할 때 UserDetails 타입으로 사용자의 정보를 객체로 관리
                //principalDetailService를 하는 이유는 따로 만들면 security랑 연동이 안된다고 함
                UserDetails userDetails =
                        principalDetailsService.loadUserByUsername(socialId);

                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, "", userDetails.getAuthorities());
                    SecurityContextHolder.getContext()
                            .setAuthentication(usernamePasswordAuthenticationToken);
                } else { //유저 없음
                    throw new AuthException(ErrorStatus.USER_NOT_FOUND);
                }
            } else { //토큰이 유효하지 않음
                throw new AuthException(ErrorStatus.AUTH_INVALID_TOKEN);
            }

            filterChain.doFilter(request, response);
        } catch (AuthException ex) {
            setJsonResponse(response, ex.getErrorReasonHttpStatus().getHttpStatus().value(),
                    ex.getErrorReason().getCode(),
                    ex.getErrorReason().getMessage());
        } catch (Exception ex) {
            setJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR",
                    "예기치 않은 오류가 발생했습니다.");
        }
    }

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
                || path.equals("/auth/login/google") //추후 개선 가능성 있음
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
