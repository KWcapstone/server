package com.kwcapstone.Service;

import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.NewProjectResponseDto;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.ProjectRepository;
import com.kwcapstone.Security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class ConferenceService {

    private final ProjectRepository projectRepository;

    public NewProjectResponseDto projectCreate(PrincipalDetails principalDetails) {
        ObjectId memberId = principalDetails.getId();

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

        NewProjectResponseDto responseDto = new NewProjectResponseDto(
                project.getProjectId(),
                project.getProjectName(),
                project.getProjectImage(),
                project.getUpdatedAt(),
                project.getCreator()
        );

        return responseDto;
    }

//    public Void scriptSave(PrincipalDetails principalDetails, ScriptMessageRequestDto requestDto) {
//
//    }
}
