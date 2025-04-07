package com.kwcapstone.Security;

import com.kwcapstone.Exception.AuthenticationException;
import com.kwcapstone.Token.JwtTokenProvider;
import jakarta.security.auth.message.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
//jwt 한 번만 filter 검사하기 위해 하는 상속
//이거 안하면 jwt 인증이 여러번 나고 그러면 성능 저하, 중복 인증 문제 등이 발생
//Spring Security 필터 중 하나로 요청에 대해 한 번만 실행되도록 보장하는 필터
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final PrincipalDetailsService principalDetailsService;

    //jwt 기반 인증 처리 필터
    //(HTTP 요청 들어올 때 jwt를 검증하고 인증정보를 저장하는 역할)
    //service에서 안하는 이유 : 비효율적, 보안적으로 좋지 않음
    //얘는 호출된 곳이 없어도 자동으로 호출하기 때문에 잇어야함
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException{


        //1. 인증이 필요 없는 url checking
        String path = request.getRequestURI();

        if (isPermitAllPath(path)) {
            filterChain.doFilter(request, response);
            return;}

        //2. jwt 검증이 필요한 경우, 추출해서 유효성 검사 및 securityContext 설정
        try {
            //jwt 추출
            String token = jwtTokenProvider.extractToken(request).trim();

            //token이 유효한지, 만료되지 않았는지 checking
            if (jwtTokenProvider.isTokenValid(token)) {
                //jwt에서 String 한 ObjectId 추출
                String memberId = jwtTokenProvider.getId(token);

                //인증을 처리할 때 UserDetails 타입으로 사용자의 정보를 객체로 관리
                //principalDetailService를 하는 이유는 따로 만들면 security랑 연동이 안된다고 함
                UserDetails userDetails =
                        principalDetailsService.loadUserByUsername(memberId);

                if (userDetails != null) {
                    //securityContext에 인증 정보 저장
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, "", userDetails.getAuthorities());
                    SecurityContextHolder.getContext()
                            .setAuthentication(usernamePasswordAuthenticationToken);
                } else { //유저 없음
                    throw new AuthenticationException(HttpStatus.NOT_FOUND.value(), "존재하지 않는 사용자입니다.");
                }
            } else { //토큰이 유효하지 않음
                throw new AuthenticationException(HttpStatus.UNAUTHORIZED.value(), "토큰이 유효하지 않습니다.");
            }

            //다음 필터 실행
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            //Jwt 검증 과정에서 인증에 실패했을 때 발생하는 예외
            setJsonResponse(response, ex.getStatusCode(), ex.getMessage());
        } catch (Exception ex) {
            //기타 에러
            setJsonResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    ex.getMessage());
        }
    }

    //오류 발생 시 Json Response
    private void setJsonResponse(HttpServletResponse response,
                                 int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        String jsonResponse = String.format
                ("{\"message\":\"%s\"}", message);
        response.getWriter().write(jsonResponse);
    }

    //token 필요 없는 url
    private boolean isPermitAllPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.equals("/swagger-ui.html")
                || path.equals("/terms.html")
                // open-api
                || path.startsWith("/api/open-api")

                // eb health check 용 API
                || path.equals("/health")

                // 로그인/토큰발급 등등
                || path.equals("/auth/sign_up")
                || path.equals("/auth/email_duplication")
                || path.equals("/auth/email_verification")
                || path.equals("/auth/login")
                || path.equals("/auth/login/kakao")
                || path.equals("/auth/login/google")
                || path.equals("/auth/login/naver")//추후 개선 가능성 있음
                || path.equals("/auth/agree")
                || path.equals("/auth/find_id")
                || path.equals("/auth/find_pw")
                || path.equals("/auth/refresh")
                || path.startsWith("/test")

                // 필요하다면 다른 permitAll 경로들도 추가
                // ...
                ;
    }


}
