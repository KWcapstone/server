package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.SaveProjectRequestDto;
import com.kwcapstone.Domain.Dto.Request.ScriptMessageRequestDto;
import com.kwcapstone.Domain.Dto.Response.NewProjectResponseDto;
import com.kwcapstone.Domain.Dto.Response.NodeUpdateResponseDto;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.ConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.ChatMessage;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
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

    // 실시간 스크립트 임시저장
    @PostMapping("/script")
    public BaseResponse<NodeUpdateResponseDto> saveScript(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                         @RequestBody ScriptMessageRequestDto requestDto) {
        return BaseResponse.res(SuccessStatus.SCRIPT_SAVE_SUCCESS,
                conferenceService.scriptSave(principalDetails, requestDto));
    }

//    @PostMapping("/conference/node")
//    public BaseResponse<NodeUpdateResponseDto> nodeUpdateMap(@AuthenticationPrincipal PrincipalDetails principalDetails,
//                                                             @RequestBody )

    // 프로젝트 저장
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse projectSave(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                    @RequestParam("projectId") String projectId,
                                    @RequestParam("scription") String scription,
                                    @RequestPart("record") MultipartFile record,
                                    @RequestPart("node") MultipartFile node) {
        SaveProjectRequestDto requestDto = new SaveProjectRequestDto(projectId, scription, record, node);
        conferenceService.saveProject(principalDetails, requestDto);
        return BaseResponse.res(SuccessStatus.PROJECT_SAVE_SUCCESS, null);
    }
}
