package com.kwcapstone.Config;

import com.kwcapstone.Token.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

//springBean 으로 등록해서 webSocketConfig에 자동 주입 ㄱㄴ
@Component
@RequiredArgsConstructor
//websocket 여결 시도할 때 실행되는 메서드
public class JwtHandShakeInterceptor implements HandshakeInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getHeader("Authorization");

            //token 형식인 애들
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    //사용자 정보 추출
                    String userId = jwtTokenProvider.getId(token);
                    attributes.put("userId", userId); // → 세션 속성에 저장
                    return true;
                } catch (Exception e) {
                    System.out.println("JWT 검증 실패: " + e.getMessage());
                }
            }
        }

        //유효하지 않은 토큰이면 webSockeet 연결 자체 거부함 (401)
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {

    }
}
