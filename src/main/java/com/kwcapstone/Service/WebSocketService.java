package com.kwcapstone.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.kwcapstone.AI.GptService;
import com.kwcapstone.Config.RoomParticipantTracker;
import com.kwcapstone.Config.WebSocketSessionRegistry;
import com.kwcapstone.Domain.Dto.Request.*;
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
import org.springframework.transaction.annotation.Transactional;
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

    private final Map<String, List<NodeDto>> sessionNodeBuffer = new ConcurrentHashMap<>();

    private final int X_BASE = 0; // 위치 기준
    private final int Y_GAP = 50;   // 노드 간 y

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
                updateNode(projectIdStr);

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

                // GPT 응답이 에러 문자열인지 확인
                if (keywordResponse.startsWith("Error:")) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "[추천 키워드] : 외부 GPT API 처리 중 오류");
                }

                // 응답 JSON 배열 → List<String> 파싱
                List<String> keywords = parseJsonArrayToList(keywordResponse);

                // 최대 5개까지만 전송
                List<String> trimmed = keywords.stream().limit(5).toList();

                messagingTemplate.convertAndSend(
                        "/topic/conference/" + projectId,
                        new RecommendKeywordDto("recommended_keywords", projectId, trimmed)
                );

            } catch (IOException e) {
                e.printStackTrace();
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

        System.out.println(summary);

        if(summary.startsWith("Error:")){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "[요약본] : 외부 GPT API 요약 처리 과정 중 오류");
        }

        ObjectMapper summaryMapper = new ObjectMapper();
        SummaryResponseDto result = pareseSummaryResponse(summary, "summary", projectId);

        messagingTemplate.convertAndSend(
                "/topic/conference/" + projectId, result);
    }

    //회의명 변경
    @Transactional
    public void modifyProjectName(String projectIdStr, ProjectNameRequestDto dto){
        String newProjectName = dto.getProjectName();
        String receiveProjectId = dto.getProjectId();

        System.out.println(receiveProjectId);
        System.out.println(projectIdStr);

        if(!receiveProjectId.equals(projectIdStr)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destination 주소 속 projectId와 message 속 projectId가 다릅니다.");
        }

        try{
            ObjectId projectIdObj = new ObjectId(projectIdStr);

            Project project = projectRepository.findByProjectId(projectIdObj)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));

            project.setProjectName(newProjectName);
            projectRepository.save(project);

            System.out.println(project.getProjectName());

            messagingTemplate.convertAndSend("/topic/conference/" + projectIdStr,
                    new ProjectNameModifyResponseDto("modifying", projectIdStr, newProjectName));
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "회의 이름 수정 중 오류");
        }
    }

    //노드 업데이트
    private void updateNode(String projectId){
        String filePath = System.getProperty("java.io.tmpdir") + "/script_" + projectId + ".txt";
        File file = new File(filePath);

        if (file.exists()) {
            try {
                // 전체 스크립트 줄 읽기
                List<String> allLines = Files.readAllLines(Path.of(filePath));
                String fullText = String.join(" ", allLines);
                String gptResult = gptService.callMindMapNode(fullText);

                ObjectMapper mapper = new ObjectMapper();
                //List<String> keywords = mapper.readValue(gptResult, new TypeReference<List<String>>() {});
                List<Map<String, Object>> gptNodes = mapper.readValue(gptResult, new TypeReference<List<Map<String, Object>>>() {});
                System.out.println("GPT 결과: " + gptResult);

                List<NodeDto> currentNodes = sessionNodeBuffer.computeIfAbsent(projectId, k -> new ArrayList<>());
                List<NodeDto> newNodes = new ArrayList<>();

                Map<String, String> idMapping = new ConcurrentHashMap<>();

                for (Map<String, Object> gptNode : gptNodes) {
                    String originalId = gptNode.get("id").toString();
                    String newId = UUID.randomUUID().toString();
                    idMapping.put(originalId, newId);
                }

                int baseY = currentNodes.size() * Y_GAP;

                for (int i = 0; i < gptNodes.size(); i++) {
                    Map<String, Object> gptNode = gptNodes.get(i);
                    String originalId = gptNode.get("id").toString();
                    String label = gptNode.get("label").toString();
                    String parentIdRaw = gptNode.get("parentId") == null ? null : gptNode.get("parentId").toString();
                    String parentId = parentIdRaw == null ? null : idMapping.get(parentIdRaw);

                    String type;
                    if (parentId == null) {
                        type = "input";
                    } else if (i == gptNodes.size() - 1) {
                        type = "output";
                    } else {
                        type = "default";
                    }

                    System.out.println("parsing이 문제?");

                    // position 추출
                    Map<String, Object> positionMap = (Map<String, Object>) gptNode.get("position");
                    int x, y;
                    if (positionMap != null && positionMap.get("x") != null && positionMap.get("y") != null) {
                        x = ((Number) positionMap.get("x")).intValue();
                        y = ((Number) positionMap.get("y")).intValue();
                    } else {
                        // fallback 값 지정 (예: 루트는 0,0 / 나머지는 순서 기반 y축 정렬)
                        x = X_BASE;
                        y = baseY + i * Y_GAP;
                    }
                    System.out.println("positon은 문제 없는데,");

                    NodeDto node = NodeDto.builder()
                            .id(idMapping.get(originalId))
                            .type(type)
                            .data(new DataDto(label))
                            .position(new PositionDto(x,y))
                            .parentId(parentId)
                            .build();

                    currentNodes.add(node);
                    newNodes.add(node);
                }

                for (NodeDto node : newNodes) {
                    messagingTemplate.convertAndSend("/topic/conference/live_on",
                            Map.of("event", "liveOn",
                                    "projectId", projectId,
                                    "node", node));
                }


                messagingTemplate.convertAndSend(
                        "/topic/conference/" + projectId,
                        new NodeUpdateResponseDto("liveOn", projectId, newNodes));
            } catch (IOException e) {
                messagingTemplate.convertAndSend(
                        "/topic/conference/" + projectId,
                        new RecommendKeywordDto("liveOn", projectId, List.of("에러 발생"))
                );
            }
        } else {
            System.out.println("파일 없음: " + filePath); // 디버깅용 로그
        }
    }

    // 키워드 직접 추가
//    public void addKeyword(String projectIdStr, MindmapAddRequestDto dto){
//        if (dto.getEvent().equals("add_mindmap")) {
//            try {
//                ObjectId projectId = new ObjectId(projectIdStr);
//                Project project = projectRepository.findByProjectId(projectId)
//                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));
//
////                String content = dto.getScription();
//
//                // 초기화
//                scriptBuffer.put(projectIdStr, new ArrayList<>());
//                newScriptionCounter.put(projectIdStr, 0);
//            } catch (IOException e) {
//                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 저장 중 오류가 발생하였습니다." + e);
//            }
//        }
//    }
}
