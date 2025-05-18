package com.kwcapstone.Controller;

import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final WebSocketService webSocketService;

    @MessageMapping("/conference/{projectId}")
    public void receiveScript(@DestinationVariable String projectId, ScriptMessageRequestDto dto) {
        webSocketService.handleScript(projectId, dto);
    }
}
