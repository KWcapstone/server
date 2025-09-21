package com.kwcapstone.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import java.nio.charset.StandardCharsets;
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
    private final Map<String, List<SaveScriptDto>> scriptBuffer = new ConcurrentHashMap<>();
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

    public void sendScript(String projectIdStr, ScriptMessageRequestDto dto){
       try{
           ObjectId projectId = new ObjectId(projectIdStr);
           Project project = projectRepository.findByProjectId(projectId)
                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));

                String content = dto.getScription();

                String time = dto.getTime();

                SaveScriptDto saveScriptDto = new SaveScriptDto(content, time);

                messagingTemplate.convertAndSend(
                        "/topic/conference/" + projectId,
                        new SendProjectResponseDto("script", projectIdStr, saveScriptDto));

       } catch (Exception e) {
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 저장 중 오류가 발생하였습니다." + e);
       }
    }

    public void saveScript(String projectIdStr, ScriptMessageRequestDto dto) {
        if (dto.getEvent().equals("gpt")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectId projectId = new ObjectId(projectIdStr);
                Project project = projectRepository.findByProjectId(projectId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));

                String content = dto.getScription();
                SaveScriptDto saveScriptDto = new SaveScriptDto(content, dto.getTime());

                //주요키워드
                sendMainKeywords(1, projectIdStr, dto, null);

                System.out.println("주요 키워드 ok ");
                //요약본
                sendSummary(projectIdStr, dto);

                System.out.println("요약본 ok ");

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
                    String jsonLine = mapper.writeValueAsString(saveScriptDto);
                    writer.write(jsonLine + System.lineSeparator());
                }

                // 누적
                scriptBuffer.computeIfAbsent(projectIdStr, k -> new ArrayList<>()).add(saveScriptDto);
                int count = newScriptionCounter.getOrDefault(projectIdStr, 0) + 1;
                newScriptionCounter.put(projectIdStr, count);

                // 추천 키워드 전송
                sendRecommendedKeywords(1, projectIdStr, null);

                System.out.println("추천 키워드 ok ");

                //노드 생성
                createNode(projectIdStr,dto.getScription());

                System.out.println("노드 키워드 ok ");

                // 초기화
                scriptBuffer.put(projectIdStr, new ArrayList<>());
                newScriptionCounter.put(projectIdStr, 0);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 저장 중 오류가 발생하였습니다." + e);
            }
        }
    }

    // 추천 키워드
    public void sendRecommendedKeywords(Integer type, String projectId, String node) {
        if(type == 1){
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
        if(type == 2){
            String keywordResponse = gptService.modifyRecommendedKeywords(node);


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
    public void sendMainKeywords(Integer type, String projectId, ScriptMessageRequestDto dto, String node){
        if(type == 1){
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

        else{
            String mainKeywords = gptService.modifyMainOpenAI(node);

            if(mainKeywords.startsWith("Error:")){
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "[주요 키워드] : 외부 GPT API 요약 처리 과정 중 오류");
            }

            List<String> result = parseJsonArrayToList(mainKeywords);

            messagingTemplate.convertAndSend(
                    "/topic/conference/" + projectId,
                    new MainKeywordDtoResponseDto("main_keywords", projectId, result));
        }

    }

    private SummaryResponseDto pareseSummaryResponse(String gptContent, String event, String projectId, String time){
        ObjectMapper mapper = new ObjectMapper();

        try{
            JsonNode node = mapper.readTree(gptContent);
            String title = node.get("title").asText();
            String content = node.get("content").asText();

            return new SummaryResponseDto(event, projectId, time, title, content);

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
        SummaryResponseDto result = pareseSummaryResponse(summary, "summary", projectId, dto.getTime());

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

    //노드 생성
    private String createNode(String projectId, String scription){
        String filePath = System.getProperty("java.io.tmpdir") + "/script_" + projectId + ".txt";
        File file = new File(filePath);

        String content = "";

        //script 파일을 찾은 후
        if (file.exists()) {
            System.out.println("파일이 존재해야함");
            //이전 scripton 불러와야함
            try {
                // 전체 스크립트 줄 읽기
                List<String> allLines = Files.readAllLines(Path.of(filePath));
                String fullText = String.join(" ", allLines);

                System.out.println("fullText" + fullText);
                System.out.println("new scipt" + scription);
                //scription과 node를 합쳐서 프롬프트를 만들어야함
                content = fullText + scription;
            }catch (IOException e){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 저장 중 오류가 발생하였습니다." + e);
            }
        } else {
            //노드 파일 만들기
            // 임시 디렉토리 경로 확인 및 생성
            String tmpDirPath = System.getProperty("java.io.tmpdir");
            File tmpDir = new File(tmpDirPath);
            if (!tmpDir.exists() && !tmpDir.mkdirs()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시 폴더 생성에 실패했습니다.");
            }

            // 임시 파일에 저장 (append 모드)
            String fileName = "script_" + projectId + ".txt";
            File newFile = new File(System.getProperty("java.io.tmpdir"), fileName);
            file = newFile;

            content = scription;
        }

        //GPT로부터 NODE 생성 부탁해야함
        try {
            String gptResult = gptService.callMindMapNode(content);

            //gpt 결과
            ObjectMapper mapper = new ObjectMapper();

            //List<String> keywords = mapper.readValue(gptResult, new TypeReference<List<String>>() {});
            List<Map<String, Object>> gptNodes = mapper.readValue(gptResult, new TypeReference<List<Map<String, Object>>>() {
            });
            System.out.println("GPT 결과: " + gptResult);

            List<NodeDto> currentNodes = sessionNodeBuffer.computeIfAbsent(projectId, k -> new ArrayList<>());
            List<NodeDto> newNodes = new ArrayList<>();

            Map<String, String> idMapping = new ConcurrentHashMap<>();

            for (Map<String, Object> gptNode : gptNodes) {
                String originalId = gptNode.get("id").toString();
                String newId = UUID.randomUUID().toString();
                idMapping.put(originalId, newId);
            }

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

                // position 추출
                Map<String, Object> positionMap = (Map<String, Object>) gptNode.get("position");
                int x, y;
                if (positionMap != null && positionMap.get("x") != null && positionMap.get("y") != null) {
                    x = ((Number) positionMap.get("x")).intValue();
                    y = ((Number) positionMap.get("y")).intValue();
                } else {
                    // fallback 값 지정 (예: 루트는 0,0 / 나머지는 순서 기반 y축 정렬)
                    x = X_BASE;
                    y = i * Y_GAP;
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
//
//            for (NodeDto node : newNodes) {
//                messagingTemplate.convertAndSend("/topic/conference/live_on",
//                        Map.of("event", "liveOn",
//                                "projectId", projectId,
//                                "node", node));
//            }

            //먼저 파일명부터 생성 및 찾기
            String nodeFilePath = System.getProperty("java.io.tmpdir") + "/node_" + projectId + ".txt";
            File nodeFile = new File(nodeFilePath);

            //없으면 만들기
            if(!nodeFile.exists()){
                //노드 파일 만들기
                // 임시 디렉토리 경로 확인 및 생성
                String tmpDirPath = System.getProperty("java.io.tmpdir");
                File tmpDir = new File(tmpDirPath);
                if (!tmpDir.exists() && !tmpDir.mkdirs()) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시 폴더 생성에 실패했습니다.");
                }

                // 임시 파일에 저장 (append 모드)
                String fileName = "node_" + projectId + ".txt";
                File newFile = new File(System.getProperty("java.io.tmpdir"), fileName);
                file = newFile;
            }

            mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(newNodes);

            //덮어씌우기
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write(json);
            }
            catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "노드 저장 중 오류가 발생하였습니다." + e);
            }

            //2. RESPONSE 보내기
            messagingTemplate.convertAndSend(
                    "/topic/conference/" + projectId,
                    new NodeUpdateResponseDto("create_node", projectId, newNodes));

            return gptResult;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    //노드 업데이트
    public void updateNode(NodeRequstDto nodeRequstDto){
        //파일에 덮어씌우기
        String result = "";

        //node가 null이 아니라면

        //1. 바뀐 노드에 대한 키워드도 다시 보내주기(이건 이후에 지워도 됨)
        sendMainKeywords(2, nodeRequstDto.getProjectId(), null, nodeRequstDto.getNodes());
        sendRecommendedKeywords(2, nodeRequstDto.getProjectId(), nodeRequstDto.getNodes());
        result = nodeRequstDto.getNodes();


        //먼저 파일명부터 생성 및 찾기
        String filePath = System.getProperty("java.io.tmpdir") + "/node_" + nodeRequstDto.getProjectId() + ".txt";
        File file = new File(filePath);

        //없으면 만들기
        if(!file.exists()){
            //노드 파일 만들기
            // 임시 디렉토리 경로 확인 및 생성
            String tmpDirPath = System.getProperty("java.io.tmpdir");
            File tmpDir = new File(tmpDirPath);
            if (!tmpDir.exists() && !tmpDir.mkdirs()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시 폴더 생성에 실패했습니다.");
            }

            // 임시 파일에 저장 (append 모드)
            String fileName = "node_" + nodeRequstDto.getProjectId() + ".txt";
            File newFile = new File(System.getProperty("java.io.tmpdir"), fileName);
            file = newFile;
        }

        //덮어씌우기
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(nodeRequstDto.getNodes());
        }
        catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "업데이트 노드 저장 중 오류가 발생하였습니다." + e);
        }


        String fileContent = null;
        try {
            fileContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "변경사항 반영된 노드 스크립트를 읽어오는데 오류가 발생했습니다." + e);
        }

        messagingTemplate.convertAndSend("/topic/conference/live_on",
                Map.of("event", "live_on",
                        "projectId", nodeRequstDto.getProjectId(),
                        "node", fileContent));
    }
}
