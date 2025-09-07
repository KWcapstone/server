package com.kwcapstone.Controller;

import com.kwcapstone.Domain.Dto.Request.*;
import com.kwcapstone.Domain.Dto.Response.RecommendKeywordDto;
import com.kwcapstone.Service.WebSocketService;
import com.kwcapstone.Config.RoomParticipantTracker;
import com.kwcapstone.Config.WebSocketSessionRegistry;
import com.kwcapstone.Domain.Dto.Response.ParticipantResponseDto;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.ChatMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final WebSocketService webSocketService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomParticipantTracker participantTracker;
    private final WebSocketSessionRegistry sessionRegistry;

    // 참여자 이름/수 업데이트
    @MessageMapping("/conference/{projectId}/modify_inviting")
    public void memberModify(@DestinationVariable String projectId, Principal principal,
                          @Payload ParticipantEventDto dto,
                          Message<?> message) {
        webSocketService.modifyMembers(projectId, dto, message);
    }

    // 실시간 스크립트 저장
    // 추천 키워드 +
    @MessageMapping("/conference/{projectId}/script")
    public void scriptSave(@DestinationVariable String projectId, Principal principal,
                           @Payload ScriptMessageRequestDto dto) {
        webSocketService.saveScript(projectId, dto);
    }

    @MessageMapping("/conference/{projectId}/modify_project_name")
    public void projectNameModify(@DestinationVariable String projectId, Principal principal,
                                  @Payload ProjectNameRequestDto dto){
        webSocketService.modifyProjectName(projectId, dto);
    }

    //node 생성
    @MessageMapping("/conference/live_on")
    public void updateNodes(Principal principal, @Payload NodeRequstDto nodeRequstDto){
        webSocketService.updateNode(nodeRequstDto);
    }
}
