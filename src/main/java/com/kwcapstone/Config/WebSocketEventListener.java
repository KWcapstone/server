package com.kwcapstone.Config;

import com.kwcapstone.Domain.Dto.Response.ParticipantResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final RoomParticipantTracker participantTracker;
    private final WebSocketSessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        WebSocketSessionRegistry.SessionInfo info = sessionRegistry.remove(sessionId);
        if (info == null) return;  // 이미 제거됐거나 알 수 없는 세션

        participantTracker.removeParticipant(info.getProjectId(), info.getMemberId());

        // 남은 인원 다시 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/conference/" + info.getProjectId() + "/participants",
                new ParticipantResponseDto(
                        "participant",
                        info.getProjectId(),
                        new ArrayList<>(participantTracker.getParticipants(info.getProjectId()))
                )
        );
    }
}
