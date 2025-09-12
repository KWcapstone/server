package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.EmailInviteRequestDto;
import com.kwcapstone.Domain.Dto.Request.ProjectDeleteRequestDto;
import com.kwcapstone.Domain.Dto.Request.ProjectNameEditRequestDto;
import com.kwcapstone.Domain.Dto.Response.InviteEmailResponseDto;
import com.kwcapstone.Domain.Dto.Response.InviteUsersByLinkResponseDto;
import com.kwcapstone.Domain.Entity.ExportKind;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
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
        InviteEmailResponseDto responseDto =
                projectService.addByEmailUser(principalDetails, projectId, emailInviteRequestDto);
        return BaseResponse.res(SuccessStatus.INVITE_EMAIL, responseDto);
    }

    // 초대 수락
    @GetMapping("/{projectId}/accept")
    public BaseResponse inviteAccept(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                     @PathVariable String projectId,
                                     @RequestParam(value = "code") String code) {
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

    //프로젝트 상태 불러오기
    @Operation(summary = "프로젝트 상태 불러오기")
    @GetMapping("/{projectId}/status")
    public BaseResponse showProjectStatus(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                         @PathVariable String projectId) {
        return BaseResponse.res(SuccessStatus.SHOW_PROJECTSTATUS, projectService.getProjectStatus(principalDetails, projectId));
    }

    //프로젝트 공유 링크로 사용자 추가
    @Operation(summary = "프로젝트 공유 링크로 사용자 추가")
    @PostMapping("/{projectId}/add_by_link")
    public BaseResponse userAddByLink(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                      @PathVariable String projectId){

        //alreadyJoin
        if(projectService.addByLink(principalDetails, projectId)){
            return BaseResponse.res(SuccessStatus.ALREADY_JOINED, null);
        }
        else{
            return BaseResponse.res(SuccessStatus.INVITE_SHARE_LINK, null);
        }
    }

    //프로젝트 추출하기
    @Operation(summary = "프로젝트 추출하기")
    @GetMapping("/{projectId}/export")
    public BaseResponse exportProject(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                      @PathVariable String projectId,
                                      @RequestParam(required = true) ExportKind kind){
        return BaseResponse.res(SuccessStatus.EXPROT_PROJECT, projectService.exportProject(principalDetails, projectId, kind));
    }
}
