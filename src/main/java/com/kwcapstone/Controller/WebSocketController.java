package com.kwcapstone.Controller;

import com.kwcapstone.Config.RoomParticipantTracker;
import com.kwcapstone.Config.WebSocketSessionRegistry;
import com.kwcapstone.Domain.Dto.Request.ParticipantRequestDto;
import com.kwcapstone.Domain.Dto.Response.ParticipantResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.ChatMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conference")
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomParticipantTracker participantTracker;
    private final WebSocketSessionRegistry sessionRegistry;

    @MessageMapping("/{roomId}/addMember")
    public void addMember(@DestinationVariable String projectId, Principal principal,
                          @Payload ParticipantRequestDto participantRequestDto,
                          Message<?> message) {
        // 1. 참가자 목록에 추가
        participantTracker.addParticipant(projectId, participantRequestDto);

        // 2. 세션 ID 추출 및 sessionRegistry 에 등록
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null) {
            String sessionId = accessor.getSessionId();
            sessionRegistry.register(sessionId, participantRequestDto.getMemberId(), projectId);
        }

        // 모든 클라이언트에게 현재 참가자 목록 전송
        messagingTemplate.convertAndSend(
                "/topic/conference/" + projectId + "/participants",
                new ParticipantResponseDto("participant", projectId,
                        new ArrayList<>(participantTracker.getParticipants(projectId)))
        );
    }

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/{roomId}")
    public ChatMessage sendRecommendedKeyword(@DestinationVariable String roomId, ChatMessage message, Principal principal) {
        return message;
    }
}
