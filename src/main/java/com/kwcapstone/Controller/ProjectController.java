package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.EmailInviteRequestDto;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main/project")
public class ProjectController {
    private final ProjectService projectService;

    // 이메일로 프로젝트에 사용자 초대
    @PostMapping("/{projectId}/add_by_email")
    public BaseResponse userAddByEmail(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                       @PathVariable String projectId,
                                       @RequestBody EmailInviteRequestDto emailInviteRequestDto) {
        projectService.addByEmailUser(principalDetails, projectId, emailInviteRequestDto);
        return BaseResponse.res(SuccessStatus.INVITE_EMAIL, null);
    }

    // 초대 수락
    @GetMapping("/{projectId}/accept")
    public BaseResponse inviteAccept(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                     @PathVariable String projectId,
                                     @RequestParam String code) {
        projectService.acceptInvite(principalDetails, projectId, code);
        return BaseResponse.res(SuccessStatus.ACCEPT_INVITE, null);
    }
}
