package com.kwcapstone.Service;

import com.kwcapstone.AI.GptService;
import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final GptService gptService;
    private final SimpMessagingTemplate simpMessagingTemplate; // stomp webSocket 메시지를 서버 클라이언트 푸쉬


    // projectId별로 스크립트를 누적 저장
    //회의 여러개가 동시에 시작되기 때문에
    //concurrentHashMap -> ,여러 스레득 도시에 접근해도 안전하게 작동
    //내부적으로 데이터를 나눠서(lock 분할) 처리해서 성능도 높고, 충돌도 방지
    private final Map<String, List<String>> scriptBuffer = new ConcurrentHashMap<>();

    //실시간 회의 스크립트를 websocket으로 받아서 GPT로 요약하고 이 요약한 것을 webSocket을 다시 클라이언트들에게 전달하는 핵심 로직
    public void handleScript(String projectId, ScriptMessageRequestDto dto) {
        scriptBuffer.computeIfAbsent(projectId, k -> new ArrayList<>()).add(dto.getContent());

        List<String> scriptList = scriptBuffer.get(projectId);
        if (scriptList.size() >= 7) {
            String fullText = String.join(" ", scriptList);
            String summary = gptService.summarizeText(fullText);

            // 요약본 WebSocket으로 전송
            messagingTemplate.convertAndSend("/topic/summary/" + projectId, summary);

            // 버퍼 초기화 or 일부만 남기기 (ex. 최근 3개는 유지 등)
            scriptBuffer.put(projectId, new ArrayList<>(scriptList.subList(scriptList.size() - 3, scriptList.size())));
        }
    }
}
