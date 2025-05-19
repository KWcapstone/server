package com.kwcapstone.Controller;

import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.RecommendKeywordDto;
import com.kwcapstone.Service.WebSocketService;
import com.kwcapstone.Config.RoomParticipantTracker;
import com.kwcapstone.Config.WebSocketSessionRegistry;
import com.kwcapstone.Domain.Dto.Request.ParticipantDto;
import com.kwcapstone.Domain.Dto.Request.ParticipantEventDto;
import com.kwcapstone.Domain.Dto.Response.ParticipantResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.ChatMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.ArrayList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conference")
public class WebSocketController {
    private final WebSocketService webSocketService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomParticipantTracker participantTracker;
    private final WebSocketSessionRegistry sessionRegistry;

    @MessageMapping("/{projectId}/modify_inviting")
    public void addMember(@DestinationVariable String projectId, Principal principal,
                          @Payload ParticipantDto participantDto,
                          Message<?> message) {
        // 1. 참가자 목록에 추가
        participantTracker.addParticipant(projectId, participantDto);

        // 2. 세션 ID 추출 및 sessionRegistry 에 등록
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null) {
            String sessionId = accessor.getSessionId();
            sessionRegistry.register(sessionId, participantDto.getMemberId(), projectId);
        }

        // 3. join 이벤트 전송
        messagingTemplate.convertAndSend(
                "/topic/conference/" + projectId + "/participants",
                new ParticipantEventDto("participant_join", projectId, participantDto)
        );
    }

    @MessageMapping("/summary/{projectId}")
    public void receiveScript(@DestinationVariable String projectId, ScriptMessageRequestDto dto) {
        webSocketService.handleScript(projectId, dto);
        // 4. 전체 목록도 갱신
        messagingTemplate.convertAndSend(
                "/topic/conference/" + projectId + "/participants",
                new ParticipantResponseDto("participants", projectId,
                        new ArrayList<>(participantTracker.getParticipants(projectId)))
        );
    }

    @MessageMapping("/{projectId}/recommend_keyword")
    @SendTo("/topic/{projectId}")
    public RecommendKeywordDto recommendedKeywordSend(@DestinationVariable String projectId) {
        return webSocketService.sendRecommendedKeywords(projectId);
    }
}
