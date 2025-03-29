package com.kwcapstone.Service;

import com.kwcapstone.Domain.Dto.Response.NoticeDetailReadResponseDto;
import com.kwcapstone.Domain.Dto.Response.NoticeReadResponseDto;
import com.kwcapstone.Domain.Dto.Response.ShowRecordResponseDto;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberToProject;
import com.kwcapstone.Domain.Entity.Notice;
import com.kwcapstone.Domain.Entity.Project;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Repository.MemberToProjectRepository;
import com.kwcapstone.Repository.NoticeRepository;
import com.kwcapstone.Repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
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

    // 알림창 전체 조회
    public List<NoticeReadResponseDto> showNotice(String memberId, String type) {
        ObjectId objectId;

        try {
            objectId = new ObjectId(memberId);
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
    public NoticeDetailReadResponseDto showDetailNotice(String memberId, String noticeId) {
        ObjectId memberObjectId;
        ObjectId noticeObjectId;

        try {
            memberObjectId = new ObjectId(memberId);
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

    // [녹음파일 + 녹음본] 메인화면
    public List<ShowRecordResponseDto> showRecording(String memberId, String sort, String filterType) {
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

            System.out.println("조회된 초대된 프로젝트 수: " + projects.size());
        } else {  // "전체 회의"인 경우
            List<MemberToProject> invitedProjectMappings = memberToProjectRepository
                    .findByMemberId(memberObjectId);

            List<ObjectId> invitedProjectIds = invitedProjectMappings.stream()
                    .map(MemberToProject::getProjectId)
                    .collect(Collectors.toList());

            projects = projectRepository.findByProjectIdInOrderByUpdatedAtDesc(invitedProjectIds);
        }

        if (projects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청한 조건에 맞는 녹음 데이터를 찾을 수 없습니다.");
        }

        try {
            Stream<ShowRecordResponseDto> recordStream = projects.stream()
                    .map(project -> {
                        String creatorName = memberRepository.findByMemberId(project.getCreator())
                                .map(Member::getName)
                                .orElse("Unknown");  // creator 정보가 없을 경우 기본값 설정

                        return new ShowRecordResponseDto(
                                project.getProjectId(),
                                project.getProjectName(),
                                project.getUpdatedAt(),
                                // project.getRecord().getFileName(),  // 음성파일 name 필요한지 잘 모르겠음
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
}
