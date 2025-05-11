package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.ProfileEditRequestDto;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.MainService;
import com.kwcapstone.Service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main")
public class MainController {
    private final MainService mainService;
    private final MemberService memberService;

    // 알림창 전체 조회
    @GetMapping("/notice")
    public BaseResponse noticeShow (@AuthenticationPrincipal PrincipalDetails principalDetails,
                                    @RequestParam(value = "type", defaultValue = "all") String type) {
        return BaseResponse.res(SuccessStatus.NOTICE_CONFIRM, mainService.showNotice(principalDetails, type));
    }

    // 알림창 세부 조회
    @GetMapping("/notice/{noticeId}")
    public BaseResponse detailNoticeShow (@AuthenticationPrincipal PrincipalDetails principalDetails,
                                          @PathVariable("noticeId") String noticeId) {
        return BaseResponse.res(SuccessStatus.NOTICE_DETAIL_CONFIRM,
                mainService.showDetailNotice(principalDetails, noticeId));
    }

    // [모든 회의] 메인화면
    @GetMapping
    public BaseResponse mainShow (@AuthenticationPrincipal PrincipalDetails principalDetails,
                                  @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                  @RequestParam(value = "filterType", defaultValue = "all") String filterType) {
        return BaseResponse.res(SuccessStatus.MAIN_SHOW , mainService.showMain(principalDetails, sort, filterType));
    }

    // [녹음파일 + 녹음본] 메인화면
    @GetMapping("/recordings")
    public BaseResponse recordingShow (@AuthenticationPrincipal PrincipalDetails principalDetails,
                                       @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                       @RequestParam(value = "filterType", defaultValue = "all") String filterType) {
        return BaseResponse.res(SuccessStatus.MAIN_RECORDING,
                mainService.showRecording(principalDetails, sort, filterType));
    }

    // [요약본] 메인화면
    @GetMapping("/summary")
    public BaseResponse summaryShow (@AuthenticationPrincipal PrincipalDetails principalDetails,
                                     @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                     @RequestParam(value = "filterType", defaultValue = "all") String filterType) {
        return BaseResponse.res(SuccessStatus.MAIN_SUMMARY,
                mainService.showSummary(principalDetails, sort, filterType));
    }

    // 탭별로 프로젝트 검색
    @GetMapping("/search")
    public BaseResponse projectSearch (@AuthenticationPrincipal PrincipalDetails principalDetails,
                                       @RequestParam(value = "tap", defaultValue = "entire") String tap,
                                       @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResponse.res(SuccessStatus.MAIN_SEARCH, mainService.searchProject(principalDetails, tap, keyword));
    }


    //프로필 모달
    @Operation(summary = "프로필 모달 띄우기")
    @GetMapping("/profile")
    public BaseResponse showProfile(@AuthenticationPrincipal PrincipalDetails principalDetails){
        ObjectId memberId = principalDetails.getId();

        return BaseResponse.res(SuccessStatus.SHOW_PROFILE, mainService.showProfile(memberId));
    }

    @Operation(summary = "프로필 수정하기")
    @PatchMapping("/profile")
    public BaseResponse editProfile(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                    ProfileEditRequestDto profileEditRequestDto){
        ObjectId memberId = principalDetails.getId();
        return BaseResponse.res(SuccessStatus.EDIT_PROFILE, mainService.editProfile(memberId, profileEditRequestDto));
    }

    //그냥 return 하기
    @Operation(summary = "테스트")
    @GetMapping("/apiTest")
    public ResponseEntity<String> healthCheck(){
        return ResponseEntity.ok("테스트 성공");
    }
}
