package com.kwcapstone.Service;

import com.kwcapstone.Domain.Dto.Request.EmailInviteRequestDto;
import com.kwcapstone.Domain.Dto.Request.ProjectDeleteRequestDto;
import com.kwcapstone.Domain.Dto.Request.ProjectNameEditRequestDto;
import com.kwcapstone.Domain.Dto.Response.GetProjectShareModalResponseDto;
import com.kwcapstone.Domain.Dto.Response.MemberInfoDto;
import com.kwcapstone.Domain.Dto.Response.ProjectNameEditResponseDto;
import com.kwcapstone.Domain.Entity.Invite;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberToProject;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.InviteRepository;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Repository.MemberToProjectRepository;
import com.kwcapstone.Repository.ProjectRepository;
import com.kwcapstone.Security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final InviteRepository inviteRepository;

    private final EmailService emailService;
    private final MemberRepository memberRepository;
    private final MemberToProjectRepository memberToProjectRepository;

    // 이메일로 프로젝트에 사용자 추가하기
    public void addByEmailUser(PrincipalDetails principalDetails,
                               String projectId, EmailInviteRequestDto emailInviteRequestDto) {
        ObjectId memberId = principalDetails.getId();
        Project project = getProject(projectId);

        // 1. 초대 코드 생성 (UUID 또는 토큰)
        String inviteCode = UUID.randomUUID().toString();

        // 2. 이메일 전송
        String inviteLink = "https://moaba.vercel.app/main/projects/" + projectId + "accept?code=" + inviteCode;

        String inviterName = principalDetails.getUsername();
        String projectName = project.getProjectName();

        emailService.sendProjectInviteMessage(
                emailInviteRequestDto.getEmail(),
                inviteLink,
                inviterName,
                projectName
        );

        saveInviteCode(inviteCode, projectId, emailInviteRequestDto.getEmail(), memberId);
    }

    // 프로젝트 찾기
    private Project getProject(String memberId) {
        ObjectId memberObjectId;
        try {
            memberObjectId = new ObjectId(memberId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식입니다.");
        }

        return projectRepository.findByProjectId(memberObjectId);
    }

    private void saveInviteCode(String inviteCode, String projectId, String email, ObjectId userId) {
        Invite invite = Invite.builder()
                .inviteCode(inviteCode)
                .projectId(new ObjectId(projectId))
                .email(email)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusHours(72))  // 유효시간 : 3일
                .build();

        inviteRepository.save(invite);
    }

    // 초대 수락
    public void acceptInvite(PrincipalDetails principalDetails, String projectId, String code) {
        // 초대 코드 검증
        Invite invite = validateInviteCode(code, projectId);

        Member invitedMember = memberRepository.findByEmail(invite.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대된 사용자가 아닙니다."));

        // principalDetails에서 ID를 가져와서 초대된 사용자의 ID와 비교
        if (!invitedMember.getMemberId().equals(principalDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "초대된 사용자가 아닙니다.");
        }

        ObjectId projectObjectId = new ObjectId(projectId);
        Project project = projectRepository.findByProjectId(projectObjectId);

        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
        }

        Member member = memberRepository.findByMemberId(principalDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        MemberToProject memberToProject = MemberToProject.builder()
                .projectId(new ObjectId(projectId))
                .memberId(member.getMemberId())
                .build();
        memberToProjectRepository.save(memberToProject);

        if (member.getProjectIds() == null) {
            member.setProjectIds(new ArrayList<>());
        }
        member.getProjectIds().add(new ObjectId(projectId));
        memberRepository.save(member);

        projectRepository.save(project);
    }

    private Invite validateInviteCode(String code, String projectId) {
        Invite invite = inviteRepository.findByInviteCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."));

        if (!invite.getProjectId().toString().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "초대 코드가 잘못되었습니다.");
        }

        if(invite.getExpiredAt().isBefore(LocalDateTime.now())){
            throw new ResponseStatusException(HttpStatus.GONE, "초대 코드가 만료되었습니다.");
        }

        return invite;
    }

    // 프로젝트 삭제 기능
    public void deleteProject(PrincipalDetails principalDetails, List<ProjectDeleteRequestDto> deleteRequestList) {
        ObjectId memberId = principalDetails.getId();

        for (ProjectDeleteRequestDto dto : deleteRequestList) {
            ObjectId projectId = dto.getProjectId();
            String type = dto.getType().toLowerCase();

            Project project = projectRepository.findByProjectId(projectId);
            if (project == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 프로젝트를 찾을 수 없습니다.");
            }

            if (!project.getCreator().equals(memberId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트 삭제 권한이 없습니다.");
            }

            if (type.equals("project")) {
                projectRepository.deleteById(projectId);
                memberToProjectRepository.deleteByProjectId(projectId);
            } else if (type.equals("record")) {
                project.setRecord(null);
                project.setScript(null);
                project.setUpdatedAt(LocalDateTime.now());
                projectRepository.save(project);
            } else if (type.equals("summary")) {
                project.setSummary(null);
                project.setUpdatedAt(LocalDateTime.now());
                projectRepository.save(project);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 삭제 타입입니다.");
            }
        }
    }

    //프로젝트 이름 수정
    public ProjectNameEditResponseDto editProjectName(String projectId,
                                                      ProjectNameEditRequestDto projectNameEditRequestDto){
        ObjectId ObjprojectId = new ObjectId(projectId);

        Project project = projectRepository.findByProjectId(ObjprojectId);

        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
        }

        project.editName(projectNameEditRequestDto.getProjectName());
        projectRepository.save(project);

        return new ProjectNameEditResponseDto(
                ObjprojectId,
                project.getProjectName());
    }

    //프로젝트 공유 모달 띄우기
    public GetProjectShareModalResponseDto getProjectShareModal(String projectId,
                                                                PrincipalDetails principalDetails) {
        ObjectId memberId = principalDetails.getId();
        ObjectId ObjprojectId = new ObjectId(projectId);

        Project project = projectRepository.findByProjectId(ObjprojectId);

        if(project == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
        }

        // 1. 초대 코드 생성 (UUID 또는 토큰)
        String inviteCode = UUID.randomUUID().toString();

        // 2. 이메일 전송
        String inviteLink = "https://moaba.vercel.app/main/projects/" + projectId + "accept?code=" + inviteCode;

        saveInviteCode(inviteCode, projectId, null, memberId);

        //4. 참여자 목록 가져오기
        List<MemberToProject> connections = memberToProjectRepository.findByProjectId(ObjprojectId);

        if(connections == null){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 참여자 목록에 null 값입니다.");
        }

        List<MemberInfoDto> sharedMembers = connections.stream()
                .map(conn -> {
                    Member member = memberRepository.findByMemberId(conn.getMemberId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없는 프로젝트 참여자입니다."));

                    //회의 생성자일때
                    if((project.getCreator()) == (member.getMemberId())) {
                        return new MemberInfoDto(member.getName(), "회의 생성자");
                    }else{
                        return new MemberInfoDto(member.getName(), "참석자");
                    }
                }).
                collect(Collectors.toList());

        return new GetProjectShareModalResponseDto(inviteLink, sharedMembers);
    }
}
