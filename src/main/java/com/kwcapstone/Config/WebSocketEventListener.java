package com.kwcapstone.Config;

import com.kwcapstone.Domain.Dto.Request.ParticipantDto;
import com.kwcapstone.Domain.Dto.Request.ParticipantEventDto;
import com.kwcapstone.Domain.Dto.Response.ParticipantResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        // 1. 해당 유저 제거
        participantTracker.removeParticipant(info.getProjectId(), info.getMemberId());

        // 2. 퇴장한 유저 정보 구성
        messagingTemplate.convertAndSend(
                "/topic/conference/" + info.getProjectId() + "/participants-event",
                new ParticipantEventDto("participant_leave", info.getProjectId(), info.getMemberId())
        );

        List<ParticipantDto> participants = participantTracker.getParticipantDtos(info.getProjectId());

        // 3. 전체 목록도 갱신
        messagingTemplate.convertAndSend(
                "/topic/conference/" + info.getProjectId() + "/participants",
                new ParticipantResponseDto("participants", info.getProjectId(),
                        String.valueOf((long) participants.size()), participants)
        );
    }
}
