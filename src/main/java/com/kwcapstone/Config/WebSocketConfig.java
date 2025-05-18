package com.kwcapstone.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


//프론트와 백엔드를 websocket+stomp로 연결할 수 있는 통로를 만들어주는 설정
//websocket 연결과 데이터만 실시간으로 주고받을 수 있는 상태를 만들 수 있음
//엔드포인트 설정 및 메시지 경로 규칙 설정 및 sockJS 지원 설정
@Configuration
@EnableWebSocketMessageBroker //STOMP 사용할 준비가 되엇다.
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//    private final JwtHandShakeInterceptor jwtHandShakeInterceptor;
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //서버가 클라이언트에게 메시지 보낼때, 해당 주소로 시작하는 주소로 브로드캐스트
        config.enableSimpleBroker("/topic");
        //클라이언트가 서버에서 요청을 보낼 때, 해당 주소로 시작하는 주소로 보냄
        config.setApplicationDestinationPrefixes("/app");
    }

    //websocket을 프론트에서 연결할 때 /ws 주소로 연결하겠다.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
//                .addInterceptors(jwtHandShakeInterceptor)
                .setAllowedOriginPatterns("*") //cors 문제 방지
                .withSockJS(); //webSocket을 브라우저가 지원못할 경우 대비한 백업용
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
