package com.kwcapstone.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kwcapstone.AI.GptService;
import com.kwcapstone.Domain.Dto.Request.SaveProjectRequestDto;
import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.*;
import com.kwcapstone.Domain.Entity.MemberToProject;
import com.kwcapstone.Domain.Entity.MindMap;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.MemberToProjectRepository;
import com.kwcapstone.Repository.ProjectRepository;
import com.kwcapstone.Security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.cglib.core.Local;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.FileWriter;
import java.io.BufferedWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private final JsonMapper jsonMapper;


    //상태 변경
    private void changeTheStatus(Project project){
        if(Objects.equals(project.getStatus(), "Before")){
            project.setStatus("Processing");

            projectRepository.save(project);
        }

        return;
    }

    public NewProjectResponseDto projectCreate(PrincipalDetails principalDetails) {
        ObjectId memberId = principalDetails.getId();
        String memberIdStr = memberId.toString();

        // 기본 프로젝트 이름 설정
        String base = "새 프로젝트";
        String sep = " ";
        String prefix = base + sep;

        List<ObjectId> joinedIds = memberToProjectRepository.findByMemberId(memberId)
                .stream().map(MemberToProject::getProjectId).toList();

        List<ObjectId> ownedIds = projectRepository.findByCreator(memberId)
                .stream().map(Project::getProjectId).toList();

        Set<ObjectId> scope = new HashSet<>();
        scope.addAll(joinedIds);
        scope.addAll(ownedIds);
        List<ObjectId> scopeIds = new ArrayList<>(scope);

        String regex = "^" + Pattern.quote(base) + "(?: (\\d+))?$";
        List<Project> candidates = scopeIds.isEmpty()
            ? Collections.emptyList()
            : projectRepository.findByProjectIdInAndProjectNameRegex(scopeIds, regex);

        Set<Integer> used = new HashSet<>();
        Pattern p = Pattern.compile(regex);

        for (Project project : candidates) {
            String name = project.getProjectName();
            Matcher m = p.matcher(name);
            if (!m.matches()) continue;

            if (m.group(1) == null) {
                used.add(0);
            } else {
                used.add(Integer.parseInt(m.group(1)));
            }
        }

        String projectName;
        if (!used.contains(0)) {
            projectName = base;
        } else {
            int k = 1;
            while (used.contains(k)) k++;
            projectName = prefix + k;
        }

        Project project = new Project();
        project.setProjectName(projectName);
        project.setProjectImage(null);
        project.setUpdatedAt(LocalDateTime.now());
        project.setCreator(memberId);
        project.setStatus("Processing");
        projectRepository.save(project);

        MemberToProject mapping = MemberToProject.builder()
                .projectId(project.getProjectId())
                .memberId(memberId)
                .build();
        memberToProjectRepository.save(mapping);

        //상태 변경
        changeTheStatus(project);

        return new NewProjectResponseDto(
                project.getProjectId().toHexString(),
                project.getProjectName(),
                project.getProjectImage(),
                project.getUpdatedAt(),
                project.getCreator().toHexString()
        );
    }

    private List<SaveScriptDto> loadScriptFromFile(String projectIdStr) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir"), "script_" + projectIdStr + ".txt");

        if(!file.exists()) {
            System.out.println("파일이 존재하지 않습니다: " + file.getAbsolutePath());
            return List.of();
        }

        ObjectMapper mapper = new ObjectMapper();
        List<SaveScriptDto> scriptLists = new ArrayList<>();
//
//        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
//        System.out.println("파일 내용:\n" + content);

        for(String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            if(line.trim().isEmpty()) {
                continue;
            }
            SaveScriptDto dto = mapper.readValue(line, SaveScriptDto.class);
            scriptLists.add(dto);
        }

        return scriptLists;
    }

    public void saveProject(PrincipalDetails principalDetails, SaveProjectRequestDto requestDto) {
        ObjectId memberId = principalDetails.getId();

        //memberId checking
        try {
            memberId = new ObjectId(String.valueOf(memberId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식입니다.");
        }

        ObjectId projectObjectId = new ObjectId(requestDto.getProjectId());

        //projectId checking
        Project project = projectRepository.findByProjectId(projectObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));


        //script dto
        List<SaveScriptDto> scriptions = new ArrayList<>();
        try{
            scriptions = loadScriptFromFile(requestDto.getProjectId());
        } catch (IOException e) {
            String message = e.getMessage();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }

        try {
            // 확장자 추출
            String nodeExt = getExtension(requestDto.getNode().getOriginalFilename());
            String recordExt = getExtension(requestDto.getRecord().getOriginalFilename());

            String projectId = requestDto.getProjectId();

            String nodeFileName = "node/" + projectId + nodeExt;
//            String recordFileName = "record/" + projectId + recordExt;

            // node
            File nodeFile = convertMultipartToFile(requestDto.getNode());
            s3Service.uploadFileToS3(nodeFileName, nodeFile);

            //스크립트
            String scriptText = requestDto.getScription();

            //summary
            String summaryText = gptService.callSummaryOpenAI(scriptText);
            String summaryFileName = "summary/" + projectId + ".txt";
            File summaryFile = createTempTextFile(summaryText);
            s3Service.uploadFileToS3(summaryFileName, summaryFile);

            //record
            File recordFile = convertMultipartToFile(requestDto.getRecord());

            //script
            String scriptFileName = "script/" + projectId + ".txt";
            File scriptFile = createTempTextFile(scriptText);

            //zip 파일 과정이 필요함
            File zipFile = createZipFile(projectId, recordFile, scriptFile);
            String zipFileName = "zip/" + projectId + ".zip";

            s3Service.uploadFileToS3(zipFileName, zipFile);

            // S3 실제 URL 생성
            String zipUrl = s3Service.getS3FileUrl(zipFileName);
            String nodeUrl = s3Service.getS3FileUrl(nodeFileName);
            String summaryUrl = s3Service.getS3FileUrl(summaryFileName);
            String scriptUrl = s3Service.getS3FileUrl(scriptFileName);

            //먼저 파일명부터 생성 및 찾기
            String filePath = System.getProperty("java.io.tmpdir") + "/node_" + requestDto.getProjectId() + ".txt";
            File file = new File(filePath);

            //없으면 만들기
            if(!file.exists()){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "임시저장된 노드 txt 파일이 없습니다");
            }

            ObjectMapper mapper = new ObjectMapper();
            List<NodeDto> nodes = mapper.readValue(file, new TypeReference<List<NodeDto>>() {});

            project.setStatus(requestDto.getStatus());

            // 프로젝트 객체 업데이트
            project.setZipFile(new Project.Zip(
                    zipUrl,
                    zipFileName,
                    scriptFile.length(),
                    recordFile.length(),
                    zipFile.length()
            ));
//            project.setRecord(new Project.Record(
//                    recordUrl,
//                    requestDto.getRecord().getOriginalFilename(),
//                    requestDto.getRecord().getSize(),  // long
//                    requestDto.getRecordLength()
//            ));

            ObjectId projectIdStr = new ObjectId(requestDto.getProjectId());
            project.setMindMap(new MindMap(
                    projectIdStr, nodes
            ));

            project.setProjectImage(nodeUrl);

            project.setScript(new Project.Script(
                    scriptUrl,
                    scriptions,
                    scriptText.getBytes(StandardCharsets.UTF_8).length
            ));

            project.setSummary(new Project.Summary(
                    summaryUrl,
                    summaryText,
                    summaryText.getBytes(StandardCharsets.UTF_8).length
            ));

            project.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            projectRepository.save(project);

            messagingTemplate.convertAndSend(
                    "/topic/conference/" + projectId,
                    "event : save"
            );

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류 발생");
        }
    }

    // .txt 파일
    private File createTempTextFile(String content) throws IOException {
        File tempFile = File.createTempFile("temp", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(content);
        }
        return tempFile;
    }

    // 확장자 추출 메서드
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 형식입니다. " + fileName);
        }
        return fileName.substring(lastDot);
    }

    public File convertMultipartToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        return file;
    }

    private File createZipFile(String projectId, File recordFile, File scriptFile) throws IOException {
        File zipFile = new File(System.getProperty("java.io.tmpdir"),  projectId + ".zip");

        try(FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos)) {
            // record 추가
            addFileToZip(recordFile, "record/" + recordFile.getName(), zos);

            // script 추가
            addFileToZip(scriptFile, "script/" + scriptFile.getName(), zos);
        }

        return zipFile;
    }

    private void addFileToZip(File file, String fileName, ZipOutputStream zos) throws IOException {
        try(FileInputStream fis = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);

            byte[] buffer =  new byte[1024];
            int length;

            while((length = fis.read(buffer)) >= 0){
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();

        }
    }

    public getProjectInfoResponseDto getDoneProject(PrincipalDetails principalDetails, String projectId){
        ObjectId memberId = principalDetails.getId();

        if(memberId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "토큰에서 넘겨진 memberId 가 null 입니다.");
        }

        ObjectId objProjectId = new ObjectId(projectId);
        Optional<Project> OpProject = projectRepository.findByProjectId(objProjectId);
        if (!OpProject.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
        }

        Project project = OpProject.get();

        String gettingContent = project.getSummary().getContent();
        JsonNode clean;
        ObjectMapper mapper = new ObjectMapper();
        try {
            clean = mapper.readTree(gettingContent);
        } catch(Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"유효하지 않은 json 형식이 저장되어있습니다.");
        }

        return new getProjectInfoResponseDto(
                projectId,
                project.getProjectName(),
                project.getUpdatedAt(),
                project.getProjectImage(),
                project.getScript().getScriptions(),
                clean
        );
    }
}