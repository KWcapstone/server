package com.kwcapstone.Service;

import com.kwcapstone.Domain.Dto.Response.NoticeReadResponseDto;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.Notice;
import com.kwcapstone.Exception.BaseException;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MainService {
    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;

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
}
