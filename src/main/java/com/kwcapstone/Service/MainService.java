package com.kwcapstone.Service;

import com.kwcapstone.Domain.Dto.Request.ProfileEditRequestDto;
import com.kwcapstone.Domain.Dto.Response.*;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberToProject;
import com.kwcapstone.Domain.Entity.Notice;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Repository.MemberToProjectRepository;
import com.kwcapstone.Repository.NoticeRepository;
import com.kwcapstone.Repository.ProjectRepository;
import com.kwcapstone.Security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@RequiredArgsConstructor
public class MainService {
    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final MemberToProjectRepository memberToProjectRepository;

    // creator name 조회
    private String findCreatorName(ObjectId creatorId) {
        Optional<Member> member = memberRepository.findById(creatorId);

        //존재 안하면
        if (!member.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식입니다.");
        }

        return member.get().getName();
    }

    // 알림창 전체 조회
    public List<NoticeReadResponseDto> showNotice(PrincipalDetails principalDetails, String type) {
        ObjectId objectId;
        ObjectId memberId = principalDetails.getId();

        try {
            objectId = new ObjectId(String.valueOf(memberId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식입니다.");
        }

        List<Notice> notices;
        if ("unRead".equalsIgnoreCase(type)) {
            notices = noticeRepository.findByUserIdAndIsReadFalseOrderByCreateAtDesc(objectId);
        } else {  // all
            notices = noticeRepository.findByUserIdOrderByCreateAtDesc(objectId);
        }

        if (notices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 알림 데이터를 찾을 수 없습니다.");
        }

        try {
            return notices.stream().map(notice -> {
                String userName = memberRepository.findByMemberId(notice.getSenderId())
                        .map(Member::getName)
                        .orElse("Unknown");  // senderId에 해당하는 유저가 없을 경우

                return new NoticeReadResponseDto(
                        notice.getNoticeId(),
                        userName,
                        notice.getTitle(),
                        notice.getIsRead()
                );
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "알림 데이터를 불러오는 중 서버에서 예상치 못한 오류가 발생했습니다.");
        }
    }

    // 알림창 세부 조회
    public NoticeDetailReadResponseDto showDetailNotice(PrincipalDetails principalDetails, String noticeId) {
        ObjectId memberObjectId;
        ObjectId noticeObjectId;
        ObjectId memberId = principalDetails.getId();

        try {
            memberObjectId = new ObjectId(String.valueOf(memberId));
            noticeObjectId = new ObjectId(noticeId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식입니다.");
        }

        Notice notice = noticeRepository.findById(noticeObjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 알림 데이터를 찾을 수 없습니다."));

        if (!notice.getUserId().equals(memberObjectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 알림을 조회할 권한이 없습니다.");
        }

        String userName = memberRepository.findByMemberId(notice.getSenderId())
                .map(Member::getName)
                .orElse("Unknown");

        if (!notice.getIsRead()) {
            notice.setIsRead(true);
            noticeRepository.save(notice);
        }

        return new NoticeDetailReadResponseDto(
                notice.getNoticeId(),
                userName,
                notice.getTitle(),
                notice.getContent()
        );
    }

    private List<Project> getProjects(String memberId, String filterType) {
        ObjectId memberObjectId;
        try {
            memberObjectId = new ObjectId(memberId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식 입니다.");
        }

        List<Project> projects = new ArrayList<>();

        if ("my".equalsIgnoreCase(filterType)) {
            projects = projectRepository.findByCreator(memberObjectId);
        } else if ("invited".equalsIgnoreCase(filterType)) {
            List<MemberToProject> invitedProjectMappings = memberToProjectRepository
                    .findByMemberId(memberObjectId);

            List<ObjectId> invitedProjectIds = invitedProjectMappings.stream()
                    .map(MemberToProject::getProjectId)
                    .collect(Collectors.toList());

            List<Project> invitedProjects = projectRepository.findByProjectIdInOrderByUpdatedAtDesc(invitedProjectIds);

            // 🔥 내가 만든 프로젝트는 제외
            List<Project> filteredProjects = invitedProjects.stream()
                    .filter(project -> !project.getCreator().equals(memberObjectId))
                    .collect(Collectors.toList());

            projects.addAll(filteredProjects);
        } else {  // "전체 회의"인 경우
            List<MemberToProject> invitedProjectMappings = memberToProjectRepository
                    .findByMemberId(memberObjectId);

            List<ObjectId> invitedProjectIds = invitedProjectMappings.stream()
                    .map(MemberToProject::getProjectId)
                    .collect(Collectors.toList());

            projects = projectRepository.findByProjectIdInOrderByUpdatedAtDesc(invitedProjectIds);
        }

        return projects;
    }

    // [모든 회의] 메인화면
    public List<ShowMainResponseDto> showMain(PrincipalDetails principalDetails, String sort, String filterType) {
        ObjectId memberId = principalDetails.getId();

        //member가 조재하는지
        Optional<Member> member = memberRepository.findById(memberId);
        if(!member.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식 입니다.");
        }

        List<Project> projects = getProjects(String.valueOf(memberId), filterType);

        if (projects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 조건에 맞는 프로젝트를 찾을 수 없습니다.");
        }


        try {
            Stream<ShowMainResponseDto> mainStream = projects.stream()
                    .map(project -> {
                        String creatorName = memberRepository.findByMemberId(project.getCreator())
                                .map(Member::getName)
                                .orElse("Unknown");
                        String strProjectId = project.getProjectId().toString();
                        return new ShowMainResponseDto(
                                strProjectId,
                                project.getProjectName(),
                                project.getUpdatedAt(),
                                creatorName,
                                project.getProjectImage()
                        );
                    });

            if ("created".equalsIgnoreCase(sort)){
                mainStream = mainStream.sorted(Comparator.comparing(ShowMainResponseDto::getUpdatedAt));
            } else {
                mainStream = mainStream.sorted(Comparator.comparing(ShowMainResponseDto::getUpdatedAt).reversed());
            }

            return mainStream.collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "프로젝트 데이터를 불러오는 중 서버에서 예상치 못한 오류가 발생했습니다.");
        }
    }

    // [녹음파일 + 녹음본] 메인화면
    public List<ShowRecordResponseDto> showRecording(PrincipalDetails principalDetails,
                                                     String sort, String filterType) {
        ObjectId memberId = principalDetails.getId();

        //member가 존재하는지
        Optional<Member> member = memberRepository.findById(memberId);
        if(!member.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식 입니다.");
        }

        List<Project> projects = getProjects(String.valueOf(memberId), filterType);

        if (projects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 조건에 맞는 녹음 데이터를 찾을 수 없습니다.");
        }

        try {
            Stream<ShowRecordResponseDto> recordStream = projects.stream()
                    .map(project -> {
                        String creatorName = memberRepository.findByMemberId(project.getCreator())
                                .map(Member::getName)
                                .orElse("Unknown");  // creator 정보가 없을 경우 기본값 설정
                        //recordId= projectId
                        String strRecordId = project.getProjectId().toString();
                        return new ShowRecordResponseDto(
                                strRecordId,
                                project.getRecord().getFileName(),
                                project.getUpdatedAt(),
                                project.getRecord().getLength(),
                                project.getScript().getSizeInBytes(),
                                creatorName
                        );
                    });
            // 정렬 조건 적용
            if ("created".equalsIgnoreCase(sort)) {
                recordStream = recordStream.sorted(Comparator.comparing(ShowRecordResponseDto::getUpdatedAt));
            } else {
                recordStream = recordStream.sorted(Comparator.comparing
                        (ShowRecordResponseDto::getUpdatedAt).reversed());
            }

            return recordStream.collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "녹음 데이터를 불러오는 중 서버에서 예상치 못한 오류가 발생했습니다.");
        }
    }

    // [요약본] 메인화면
    public List<ShowSummaryResponseDto> showSummary(PrincipalDetails principalDetails,
                                                    String sort, String filterType) {
        ObjectId memberId = principalDetails.getId();
        //member가 존재하는지
        Optional<Member> member = memberRepository.findById(memberId);
        if(!member.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식 입니다.");
        }

        List<Project> projects = getProjects(String.valueOf(memberId), filterType);

        if (projects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 조건에 맞는 요약본 데이터를 찾을 수 없습니다.");
        }

        try {
            Stream<ShowSummaryResponseDto> summaryStream = projects.stream()
                    .map(project -> {
                        String creatorName = memberRepository.findByMemberId(project.getCreator())
                                .map(Member::getName)
                                .orElse("Unknown");
                        //recordId= projectId
                        String strRecordId = project.getProjectId().toString();

                        return new ShowSummaryResponseDto(
                                strRecordId,
                                project.getProjectName(),
                                project.getUpdatedAt(),
                                creatorName,
                                project.getSummary().getSizeInBytes()
                        );
                    });
            if ("created".equalsIgnoreCase(sort)) {
                summaryStream = summaryStream.sorted(Comparator.comparing(ShowSummaryResponseDto::getUpdatedAt));
            } else {
                summaryStream = summaryStream.sorted(Comparator.comparing
                        (ShowSummaryResponseDto::getUpdatedAt).reversed());
            }

            return summaryStream.collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "요약본 데이터를 불러오는 중 서버에서 예상치 못한 오류가 발생했습니다.");
        }
    }

    // 탭별로 검색
    public List<SearchResponseWrapperDto> searchProject(PrincipalDetails principalDetails, String tap, String keyword) {
        // 1. 멤버의 아이디를 통해 이게 존재하는 아이디인지 검색
        ObjectId memberId = principalDetails.getId();
        Optional<Member> member = memberRepository.findByMemberId(memberId);
        if(!member.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ObjectId 형식 입니다.");
        }

        // 2. 그 멤버의 프로젝트를 가져옴.
        List<MemberToProject> invitedProjectMappings = memberToProjectRepository
                .findByMemberId(memberId);

        //초대돈 프로젝트 ID불러오기
        List<ObjectId> invitedProjectIds = invitedProjectMappings.stream()
                .map(MemberToProject::getProjectId)
                .collect(Collectors.toList());

        //그를 토대로 프로젝트 불러오기
        List<Project> projects = projectRepository.findByProjectIdInOrderByUpdatedAtDesc(invitedProjectIds);

        //프로젝트 없을 경우
        if (projects.isEmpty()) {
            return null;
        }

        // 3. 탭으로 필터링
        List<SearchResponseWrapperDto> result = new ArrayList<>();


        for (Project project : projects) {
            //dto에 집어넣기
            SearchResponseWrapperDto dto = new SearchResponseWrapperDto();
            dto.setTap(tap);
            String strprojectId = project.getProjectId().toString();
            dto.setProjectId(strprojectId);
            dto.setProjectName(project.getProjectName());
            dto.setUpdatedAt(project.getUpdatedAt());
            String creatorName = memberRepository.findByMemberId(project.getCreator())
                    .map(Member::getName)
                    .orElse("Unknown");  // creator 정보가 없을 경우 기본값 설정
            dto.setCreator(creatorName);

            if ("entire".equalsIgnoreCase(tap)) {
                if (keyword != null && !keyword.isBlank()
                        && (project.getProjectName() == null
                        || !project.getProjectName().toLowerCase().contains(keyword.toLowerCase()))) {
                    continue;
                }
                dto.setResult(List.of(new SearchResponseWrapperDto.EntireDto(project.getProjectImage())));
                result.add(dto);
            } else if ("record".equalsIgnoreCase(tap)) {
                Project.Record record = project.getRecord();
                if (record == null) continue;

                if (keyword != null && !keyword.isBlank()
                        && (project.getProjectName() == null
                        || !record.getFileName().toLowerCase().contains(keyword.toLowerCase()))) {
                    continue;
                }

                dto.setResult(List.of(
                        new SearchResponseWrapperDto.RecordDto(
                                record.getLength(),
                                project.getScript() != null ? project.getScript().getSizeInBytes(): 0L
                        )
                ));
                result.add(dto);
            } else if ("summary".equalsIgnoreCase(tap)) {
                Project.Summary summary = project.getSummary();
                if (summary == null) continue;

                if (keyword != null && !keyword.isBlank()
                        && (project.getProjectName() == null
                        || !summary.getContent().toLowerCase().contains(keyword.toLowerCase()))) {
                    continue;
                }

                dto.setResult(List.of(new SearchResponseWrapperDto.SummaryDto(summary.getSizeInBytes())));
                result.add(dto);
            }
        }

        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 조건에 맞는 프로젝트를 찾을 수 없습니다.");
        }

        return result;
    }

    //프로필 보이기
    public ProfileResponseDto showProfile(ObjectId memberId){
        if(memberId == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberId가 null 입니다.");
        }
        Optional<Member> member = memberRepository.findByMemberId(memberId);

        if(!member.isPresent()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원입니다.");
        }

        String strmemberId = memberId.toString();
        ProfileResponseDto profileResponseDto = new ProfileResponseDto(
                strmemberId,
                member.get().getName(),
                member.get().getEmail(),
                member.get().getImage());

        return profileResponseDto;
    }

    //프로필 수정하기
    public ProfileResponseDto editProfile(ObjectId memberId, ProfileEditRequestDto profileEditRequestDto){
        if(memberId == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberId가 null 입니다.");
        }

        Optional<Member> member = memberRepository.findByMemberId(memberId);
        if(!member.isPresent()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원입니다.");
        }

        member.get().editProfie(profileEditRequestDto);

        memberRepository.save(member.get());

        String strmemberId = memberId.toString();
        return new ProfileResponseDto(
                strmemberId,
                member.get().getName(),
                member.get().getEmail(),
                member.get().getImage()
        );
    }
}
