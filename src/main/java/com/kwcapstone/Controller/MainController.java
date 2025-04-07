package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main")
public class MainController {
    private final MainService mainService;

    // 알림창 전체 조회
    @GetMapping("/notice/{memberId}")
    public BaseResponse noticeShow (@PathVariable("memberId") String memberId,
                                    @RequestParam(value = "type", defaultValue = "all") String type) {
        return BaseResponse.res(SuccessStatus.NOTICE_CONFIRM, mainService.showNotice(memberId, type));
    }

    // 알림창 세부 조회
    @GetMapping("/notice/{memberId}/{noticeId}")
    public BaseResponse detailNoticeShow (@PathVariable("memberId") String memberId,
                                          @PathVariable("noticeId") String noticeId) {
        return BaseResponse.res(SuccessStatus.NOTICE_DETAIL_CONFIRM, mainService.showDetailNotice(memberId, noticeId));
    }

    // [모든 회의] 메인화면
    @GetMapping("/{memberId}")
    public BaseResponse mainShow (@PathVariable("memberId") String memberId,
                                  @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                  @RequestParam(value = "filterType", defaultValue = "all") String filterType) {
        return BaseResponse.res(SuccessStatus.MAIN_SHOW , mainService.showMain(memberId, sort, filterType));
    }

    // [녹음파일 + 녹음본] 메인화면
    @GetMapping("/recordings/{memberId}")
    public BaseResponse recordingShow (@PathVariable("memberId") String memberId,
                                       @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                       @RequestParam(value = "filterType", defaultValue = "all") String filterType) {
        return BaseResponse.res(SuccessStatus.MAIN_RECORDING, mainService.showRecording(memberId, sort, filterType));
    }

    // [요약본] 메인화면
    @GetMapping("/summary/{memberId}")
    public BaseResponse summaryShow (@PathVariable("memberId") String memberId,
                                     @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                     @RequestParam(value = "filterType", defaultValue = "all") String filterType) {
        return BaseResponse.res(SuccessStatus.MAIN_SUMMARY, mainService.showSummary(memberId, sort, filterType));
    }

    // 탭별로 프로젝트 검색
    @GetMapping("/search/{memberId}")
    public BaseResponse projectSearch (@PathVariable("memberId") String memberId,
                                       @RequestParam(value = "tap", defaultValue = "entire") String tap,
                                       @RequestParam(value = "keyword") String keyword) {
        return BaseResponse.res(SuccessStatus.MAIN_SEARCH, mainService.searchProject(memberId, tap, keyword));
    }
}
