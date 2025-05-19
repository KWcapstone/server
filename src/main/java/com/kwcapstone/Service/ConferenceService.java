package com.kwcapstone.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.AI.GptService;
import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.*;
import com.kwcapstone.Domain.Entity.MemberToProject;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.MemberToProjectRepository;
import com.kwcapstone.Repository.ProjectRepository;
import com.kwcapstone.Security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class ConferenceService {

    private final ProjectRepository projectRepository;
    private final S3Service s3Service;
    private final MemberToProjectRepository memberToProjectRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GptService gptService;
    private final WebSocketService webSocketService;

    private final Map<String, List<String>> scriptBuffer = new ConcurrentHashMap<>();
    private final Map<String, Integer> newScriptionCounter = new ConcurrentHashMap<>();
    private final Map<String, List<NodeDto>> sessionNodeBuffer = new ConcurrentHashMap<>();
    private final int X_BASE = 100; // 위치 기준
    private final int Y_GAP = 80;   // 노드 간 y 간격


    public NewProjectResponseDto projectCreate(PrincipalDetails principalDetails) {
        ObjectId memberId = principalDetails.getId();
        String memberIdStr = memberId.toString();

        // 기본 프로젝트 이름 설정
        String baseProjectName = "새 프로젝트";
        String projectName = baseProjectName;
        int count = 1;

        // 동일한 프로젝트 이름이 이미 존재하는지 확인
        while (projectRepository.existsByProjectName(projectName)) {
            projectName = baseProjectName + " " + count++;
        }

        Project project = new Project();
        project.setProjectName(projectName);
        project.setProjectImage(null);
        project.setUpdatedAt(LocalDateTime.now());
        project.setCreator(memberId);
        projectRepository.save(project);

        MemberToProject mapping = MemberToProject.builder()
                .projectId(project.getProjectId())
                .memberId(memberId)
                .build();
        memberToProjectRepository.save(mapping);

        return new NewProjectResponseDto(
                project.getProjectId().toHexString(),
                project.getProjectName(),
                project.getProjectImage(),
                project.getUpdatedAt(),
                project.getCreator().toHexString()
        );
    }

    public NodeUpdateResponseDto scriptSave(PrincipalDetails principalDetails, ScriptMessageRequestDto requestDto) {
        ObjectId memberId = principalDetails.getId();
        try {
            String projectIdStr = requestDto.getProjectId();
            ObjectId projectId = new ObjectId(projectIdStr);

            Project project = projectRepository.findByProjectId(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));

            if (!project.getCreator().equals(memberId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 프로젝트 대한 권한이 없습니다.");
            }

            String content = requestDto.getScription();

            // 임시 디렉토리 경로 확인 및 생성
            String tmpDirPath = System.getProperty("java.io.tmpdir");
            File tmpDir = new File(tmpDirPath);
            if (!tmpDir.exists() && !tmpDir.mkdirs()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시 폴더 생성에 실패했습니다.");
            }

            // 임시 파일에 저장 (append 모드)
            String fileName = "script_" + projectId + ".txt";
            File file = new File(System.getProperty("java.io.tmpdir"), fileName);

            try(FileWriter writer = new FileWriter(file, true)) {
                writer.write(content + "\n");
            }
            // S3에 업로드 (덮어쓰기 형식)
            String s3Path = "scripts/" + fileName;
            s3Service.uploadFileToS3(s3Path, file);

            // 누적
            scriptBuffer.computeIfAbsent(projectIdStr, k -> new ArrayList<>()).add(content);
            int count = newScriptionCounter.getOrDefault(projectIdStr, 0) + 1;
            newScriptionCounter.put(projectIdStr, count);

                // GPT 호출 조건 (7문장마다)
                List<String> scriptList = scriptBuffer.get(projectIdStr);
                String fullText = String.join(" ", scriptList);
                String gptResult = gptService.callMindMapNode(fullText);

                ObjectMapper mapper = new ObjectMapper();
                List<String> keywords = mapper.readValue(gptResult, new TypeReference<List<String>>() {});

                List<NodeDto> currentNodes = sessionNodeBuffer.computeIfAbsent(projectIdStr, k -> new ArrayList<>());
                List<NodeDto> newNodes = new ArrayList<>();
                int baseY = currentNodes.size() * Y_GAP;

                boolean isFirstNode = sessionNodeBuffer.get(projectIdStr) == null || sessionNodeBuffer.get(projectIdStr).isEmpty();

                for (int i = 0; i < keywords.size(); i++) {
                    String keyword = keywords.get(i);

                    String type;
                    if (isFirstNode && i == 0) {
                        type = "input"; // 처음 노드만 input
                    } else if (i == keywords.size() - 1) {
                        type = "output"; // 마지막 노드
                    } else {
                        type = "default"; // 그 외
                    }

                    NodeDto node = NodeDto.builder()
                            .id(UUID.randomUUID().toString())
                            .type(type)
                            .data(new DataDto(keyword))
                            .position(new PositionDto(X_BASE, baseY + i * Y_GAP))
                            .parentId(null) // 이후 연결할 경우 지정
                            .build();
                    currentNodes.add(node);
                    newNodes.add(node);
                }

                // 클라이언트에 변경 노드만 전송
                for (NodeDto node : newNodes) {
                    messagingTemplate.convertAndSend("/topic/conference/live_on" ,
                            Map.of("event", "liveOn",
                                    "projectId", projectIdStr,
                                    "node", node));

                    // ✅ 콘솔 확인용 로그
                    System.out.println("[Generated Node]");
                    System.out.println(" - id: " + node.getId());
                    System.out.println(" - type: " + node.getType());
                    System.out.println(" - label: " + node.getData().getLabel());
                    System.out.println(" - position: (" + node.getPosition().getX() + ", " + node.getPosition().getY() + ")");
                }

                // 초기화
                scriptBuffer.put(projectIdStr, new ArrayList<>());
                newScriptionCounter.put(projectIdStr, 0);
                NodeUpdateResponseDto update =
                        NodeUpdateResponseDto.builder()
                                .event("live_on")
                                .projectId(projectIdStr)
                                .nodes(newNodes)
                                .build();
            return update;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 저장 중 오류가 발생하였습니다.");
        }
    }
}
// 67b47076c89fe04ae30dc7ba