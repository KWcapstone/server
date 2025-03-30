package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Service.MainService;
import lombok.RequiredArgsConstructor;
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

    // [녹음파일 + 녹음본] 메인화면
    @GetMapping("/recordings/{memberId}")
    public BaseResponse recordingShow (@PathVariable("memberId") String memberId,
                                       @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                       @RequestParam(value = "filterType", defaultValue = "all") String filterType) {
        return BaseResponse.res(SuccessStatus.MAIN_RECORDING, mainService.showRecording(memberId, sort, filterType));
    }
}
