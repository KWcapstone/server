package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.SaveProjectRequestDto;
import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.NodeUpdateResponseDto;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.ConferenceService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conference")
public class ConferenceController {
    private final ConferenceService conferenceService;

    // 회의 화면 (프로젝트 생성)
    @PostMapping
    public BaseResponse createProject(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return BaseResponse.res(SuccessStatus.NEW_PROJECT,
                conferenceService.projectCreate(principalDetails));
    }


    // 프로젝트 저장
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse projectSave(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                    @RequestParam("projectId") String projectId,
                                    @RequestParam("scription") String scription,
                                    @RequestPart("record") MultipartFile record,
                                    @RequestPart("recordLength") String recordLength,
                                    @RequestPart("node") MultipartFile node) {
        SaveProjectRequestDto requestDto = new SaveProjectRequestDto(projectId, scription, record, recordLength, node);
        conferenceService.saveProject(principalDetails, requestDto);
        return BaseResponse.res(SuccessStatus.PROJECT_SAVE_SUCCESS, null);
    }

    //끝난 프로젝트 보이기
    @Operation(summary = "끝난 프로젝트 보이기")
    @GetMapping("/view/{projectId}")
    public BaseResponse showTheDoneProject(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                           @PathVariable String projectId){
        return BaseResponse.res(SuccessStatus.SHOW_DONE_PROJECT, conferenceService.getDoneProject(principalDetails, projectId));
    }
}
