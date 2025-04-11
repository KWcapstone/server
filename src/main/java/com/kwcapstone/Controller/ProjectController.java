package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.EmailInviteRequestDto;
import com.kwcapstone.Domain.Dto.Request.ProjectDeleteRequestDto;
import com.kwcapstone.Domain.Dto.Request.ProjectNameEditRequestDto;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 프로젝트 삭제
    @DeleteMapping("/delete")
    public BaseResponse projectDelete(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                      @RequestBody List<ProjectDeleteRequestDto> deleteRequestList) {
        projectService.deleteProject(principalDetails, deleteRequestList);
        return BaseResponse.res(SuccessStatus.DELETE_PROJECT, null);
    }

    //프로젝트 이름 수정
    @Operation(summary = "프로젝트 이름 수정")
    @PatchMapping("/{projectId}/edit")
    public BaseResponse projectNameEdit(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                        @PathVariable String projectId,
                                        @RequestBody ProjectNameEditRequestDto projectNameEditRequestDto) {
        return BaseResponse.res(SuccessStatus.EDIT_PROJECT_NAME,
                projectService.editProjectName(projectId, projectNameEditRequestDto));
    }

    //프로젝트 공유모달 띄우기
    @Operation(summary = "프로젝트 공유모달 띄우기")
    @GetMapping("/{projectId}")
    public BaseResponse getProjectShareModel(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                             @PathVariable String projectId) {
        return BaseResponse.res(SuccessStatus.SHOW_PROJECTSHARE, projectService.getProjectShareModal(projectId, principalDetails));
    }

    //프로젝트 공유 링크로 사용자 추가
    @Operation(summary = "프로젝트 공유 링크로 사용자 추가")
    @PostMapping("/{projectId}/add_by_link")
    public BaseResponse userAddByLink(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                      @PathVariable String projectId,
                                      @RequestParam String code){
        if(projectService.addByLink(principalDetails, projectId, code) == null){

        }
    }
}
