package com.kwcapstone.Service;

import com.kwcapstone.AI.GptService;
import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final GptService gptService;
    private final SimpMessagingTemplate simpMessagingTemplate; // stomp webSocket 메시지를 서버 클라이언트 푸쉬

    public List<String> parseJsonArrayToList(String jsonArray) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonArray, List.class);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of("파싱 실패");
        }
    }

    // projectId별로 스크립트를 누적 저장
    //회의 여러개가 동시에 시작되기 때문에
    //concurrentHashMap -> ,여러 스레득 도시에 접근해도 안전하게 작동
    //내부적으로 데이터를 나눠서(lock 분할) 처리해서 성능도 높고, 충돌도 방지
    private final Map<String, List<String>> scriptBuffer = new ConcurrentHashMap<>();
    private final Map<String, Integer> newScriptionCounter = new ConcurrentHashMap<>();

    //실시간 회의 스크립트를 websocket으로 받아서 GPT로 요약하고 이 요약한 것을 webSocket을 다시 클라이언트들에게 전달하는 핵심 로직
    public void handleScript(String projectId, ScriptMessageRequestDto dto) {
        //scriptBuffer는 Map<Project, 스크립트 리스트> 구조
        //스크립트에 새로운 문장 추가
        scriptBuffer.computeIfAbsent(projectId, k -> new ArrayList<>()).add(dto.getScription());

        //projctId를 통해 현재 회의바에 저장딘 전체 스크립트 리스트를 가져옴
        List<String> scriptList = scriptBuffer.get(projectId);

        // 신규 문장 카운트 증가
        int currentCount = newScriptionCounter.getOrDefault(projectId, 0) + 1;
        newScriptionCounter.put(projectId, currentCount);

        //새로운 문장이 7개 ㅇ상이면 gpt 호출
        if (currentCount >=  7) {
            String fullText = String.join(" ", scriptList);
            String summary = gptService.callSummaryOpenAI(fullText);

            // 요약본 WebSocket으로 전송
            simpMessagingTemplate.convertAndSend("/topic/summary/" + projectId,
                    Map.of(
                            "event", "summary",
                            "projectId", projectId,
                            "summary", summary
                    ));

            newScriptionCounter.put(projectId, 0);
        }
    }

    //실시간 회의 스크립트를 websocket으로 받아서 GPT로 요약하고 이 요약한 것을 webSocket을 다시 클라이언트들에게 전달하는 핵심 로직
    public void handleMain(String projectId, ScriptMessageRequestDto dto) {
        //scriptBuffer는 Map<Project, 스크립트 리스트> 구조
        //스크립트에 새로운 문장 추가
//        scriptBuffer.computeIfAbsent(projectId, k -> new ArrayList<>()).add(dto.getScription());

        //projctId를 통해 현재 회의바에 저장딘 전체 스크립트 리스트를 가져옴
        List<String> scriptList = scriptBuffer.get(projectId);

        // 신규 문장 카운트 증가
        int currentCount = newScriptionCounter.getOrDefault(projectId, 0) + 1;
//        newScriptionCounter.put(projectId, currentCount);

        //새로운 문장이 7개 ㅇ상이면 gpt 호출
        if (currentCount >=  7) {
            String fullText = String.join(" ", scriptList);
            String mainKeyword = gptService.callMainOpenAI(fullText);
            List<String> keywords = parseJsonArrayToList(mainKeyword);

            // 요약본 WebSocket으로 전송
            simpMessagingTemplate.convertAndSend("/topic/main_keyword/" + projectId,
                    Map.of(
                            "event", "main_keywords",
                            "projectId", projectId,
                            "summary", keywords
                    ));

            newScriptionCounter.put(projectId, 0);
        }
    }
}
