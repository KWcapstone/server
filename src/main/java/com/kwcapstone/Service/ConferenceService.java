package com.kwcapstone.Service;

import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.NewProjectResponseDto;
import com.kwcapstone.Domain.Entity.MemberToProject;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.MemberToProjectRepository;
import com.kwcapstone.Repository.ProjectRepository;
import com.kwcapstone.Security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ConferenceService {

    private final ProjectRepository projectRepository;
    private final S3Service s3Service;
    private final MemberToProjectRepository memberToProjectRepository;

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

    public Void scriptSave(PrincipalDetails principalDetails, ScriptMessageRequestDto requestDto) {
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

            return null;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 저장 중 오류가 발생하였습니다.");
        }
    }
}
// 67b47076c89fe04ae30dc7ba