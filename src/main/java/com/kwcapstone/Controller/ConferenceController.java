package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Response.NewProjectResponseDto;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.ConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conference")
public class ConferenceController {
    private final ConferenceService conferenceService;
//        // 회원가입
//    @PostMapping("/sign_up")
//    public BaseResponse signUp(@RequestBody MemberRequestDto memberRequestDto) {
//        memberService.join(memberRequestDto);
//        //null 이면 response 응답기에서 알아서 null 인지해서 응답 필드에서 빼버림
//        return BaseResponse.res(SuccessStatus.USER_SIGN_UP,null);
//    }

//        // 알림창 전체 조회
//    @GetMapping("/notice/{memberId}")
//    public BaseResponse noticeShow (@PathVariable("memberId") String memberId,
//                                    @RequestParam(value = "type", defaultValue = "all") String type) {
//        return BaseResponse.res(SuccessStatus.NOTICE_CONFIRM, mainService.showNotice(memberId, type));
//    }

    // 회의 화면 (프로젝트 생성)
    // 이거 회의 아이디랑 (다른 conference 부분, 멤버 아이디 상의해야할듯)
    @PostMapping
    public BaseResponse createProject (@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return BaseResponse.res(SuccessStatus.NEW_PROJECT,
                conferenceService.projectCreate(principalDetails));
    }
}
