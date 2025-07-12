package com.kwcapstone.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.kwcapstone.AI.GptService;
import com.kwcapstone.Config.RoomParticipantTracker;
import com.kwcapstone.Config.WebSocketSessionRegistry;
import com.kwcapstone.Domain.Dto.Request.ParticipantDto;
import com.kwcapstone.Domain.Dto.Request.ParticipantEventDto;
import com.kwcapstone.Domain.Dto.Request.ProjectNameRequestDto;
import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.*;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final GptService gptService;
    private final SimpMessagingTemplate simpMessagingTemplate; // stomp webSocket 메시지를 서버 클라이언트 푸쉬
    private final Map<String, Integer> lastProcessedLineCount = new ConcurrentHashMap<>();
    private final RoomParticipantTracker participantTracker;
    private final WebSocketSessionRegistry sessionRegistry;
    private final ProjectRepository projectRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // projectId별로 스크립트를 누적 저장
    //회의 여러개가 동시에 시작되기 때문에
    //concurrentHashMap -> ,여러 스레득 도시에 접근해도 안전하게 작동
    //내부적으로 데이터를 나눠서(lock 분할) 처리해서 성능도 높고, 충돌도 방지
    private final Map<String, List<String>> scriptBuffer = new ConcurrentHashMap<>();
    private final Map<String, Integer> newScriptionCounter = new ConcurrentHashMap<>();

    public void modifyMembers(String projectId, ParticipantEventDto dto, Message<?> message) {
        // 참가자 추가하는 경우
        if (dto.getEvent().equals("participant_join")) {
            participantTracker.addParticipant(projectId, dto.getMemberId());
        }

        // 참가자 제외하는 경우
        if (dto.getEvent().equals("participant_leave")) {
            participantTracker.removeParticipant(projectId, dto.getMemberId());
        }

        // 세션 ID 추출 및 sessionRegistry 에 등록
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null) {
            String sessionId = accessor.getSessionId();
            sessionRegistry.register(sessionId, dto.getMemberId(), dto.getProjectId());
        }

        List<ParticipantDto> participants = participantTracker.getParticipantDtos(dto.getProjectId());

        messagingTemplate.convertAndSend(
                "/topic/conference/" + projectId,
                new ParticipantResponseDto("participants", dto.getProjectId(),
                        String.valueOf((long) participants.size()), participants)
        );
    }

    public void saveScript(String projectIdStr, ScriptMessageRequestDto dto) {
        if (dto.getEvent().equals("script")) {
            try {
                ObjectId projectId = new ObjectId(projectIdStr);
                Project project = projectRepository.findByProjectId(projectId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));

                String content = dto.getScription();
                //주요키워드
                sendMainKeywords(projectIdStr, dto);

                //요약본
                sendSummary(projectIdStr, dto);

                // 임시 디렉토리 경로 확인 및 생성
                String tmpDirPath = System.getProperty("java.io.tmpdir");
                File tmpDir = new File(tmpDirPath);
                if (!tmpDir.exists() && !tmpDir.mkdirs()) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시 폴더 생성에 실패했습니다.");
                }

                // 임시 파일에 저장 (append 모드)
                String fileName = "script_" + projectIdStr + ".txt";
                File file = new File(System.getProperty("java.io.tmpdir"), fileName);

                try(FileWriter writer = new FileWriter(file, true)) {
                    writer.write(content + "\n");
                }

                // 누적
                scriptBuffer.computeIfAbsent(projectIdStr, k -> new ArrayList<>()).add(content);
                int count = newScriptionCounter.getOrDefault(projectIdStr, 0) + 1;
                newScriptionCounter.put(projectIdStr, count);

                // 추천 키워드 전송
                sendRecommendedKeywords(projectIdStr);

                // 초기화
                scriptBuffer.put(projectIdStr, new ArrayList<>());
                newScriptionCounter.put(projectIdStr, 0);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 저장 중 오류가 발생하였습니다." + e);
            }
        }
    }

    // 추천 키워드
    public void sendRecommendedKeywords(String projectId) {
        String filePath = System.getProperty("java.io.tmpdir") + "/script_" + projectId + ".txt";
        File file = new File(filePath);

        if (file.exists()) {
            try {
                // 전체 스크립트 줄 읽기
                List<String> allLines = Files.readAllLines(Path.of(filePath));
                String fullText = String.join(" ", allLines);
                String keywordResponse = gptService.callRecommendedKeywords(fullText);

                List<String> keywords = Arrays.stream(keywordResponse.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(5)
                        .collect(Collectors.toList());

                messagingTemplate.convertAndSend(
                        "/topic/conference/" + projectId,
                        new RecommendKeywordDto("recommended_keywords", projectId, keywords));
                } catch (IOException e) {
                messagingTemplate.convertAndSend(
                        "/topic/conference/" + projectId,
                        new RecommendKeywordDto("recommended_keywords", projectId, List.of("에러 발생"))
                );
            }
        } else {
            System.out.println("파일 없음: " + filePath); // 디버깅용 로그
        }
    }

    public List<String> parseJsonArrayToList(String jsonArray) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonArray, List.class);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of("파싱 실패");
        }
    }

    //주요키워드
    public void sendMainKeywords(String projectId, ScriptMessageRequestDto dto){
        String content = dto.getScription();

        String mainKeywords = gptService.callMainOpenAI(content);

        if(mainKeywords.startsWith("Error:")){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "[주요 키워드] : 외부 GPT API 요약 처리 과정 중 오류");
        }

        List<String> result = parseJsonArrayToList(mainKeywords);

        messagingTemplate.convertAndSend(
                "/topic/conference/" + projectId,
                new MainKeywordDtoResponseDto("main_keywords", projectId, result));

    }

    private SummaryResponseDto pareseSummaryResponse(String gptContent, String event, String projectId){
        ObjectMapper mapper = new ObjectMapper();

        try{
            JsonNode node = mapper.readTree(gptContent);
            String title = node.get("title").asText();
            String content = node.get("content").asText();

            return new SummaryResponseDto(event, projectId, title, content);

        }catch (Exception e){
            e.printStackTrace();

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "[요약본] : 요약본 result 파싱 중 오류");
        }
    }

    //요약
    public void sendSummary(String projectId, ScriptMessageRequestDto dto){
        String content = dto.getScription();

        String summary = gptService.callSummaryOpenAI(content);

        if(summary.startsWith("Error:")){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "[요약본] : 외부 GPT API 요약 처리 과정 중 오류");
        }

        ObjectMapper summaryMapper = new ObjectMapper();
        SummaryResponseDto result = pareseSummaryResponse(summary, "summary", projectId);

        messagingTemplate.convertAndSend(
                "/topic/conference/" + projectId, result);
    }

    //회의명 변경
    public void modifyProjectName(String projectId, ProjectNameRequestDto dto){

    }
}
