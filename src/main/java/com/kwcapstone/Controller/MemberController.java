package com.kwcapstone.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.*;
import com.kwcapstone.Domain.Dto.Response.MemberLoginResponseDto;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Security.PrincipalDetails;
import com.kwcapstone.Service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class MemberController {
    @Autowired
    private MemberRepository memberRepository;

    private final MemberService memberService;
    @Autowired
    private HttpSession httpSession;

    // 회원가입
    @PostMapping("/sign_up")
    public BaseResponse signUp(@RequestBody MemberRequestDto memberRequestDto) {
        memberService.join(memberRequestDto);
        //null 이면 response 응답기에서 알아서 null 인지해서 응답 필드에서 빼버림
        return BaseResponse.res(SuccessStatus.USER_SIGN_UP,null);
    }

    // 이메일 중복 인증
    @PostMapping("/email_duplication")
    public BaseResponse emailDuplication(@RequestBody EmailDuplicationDto emailDuplicationDto) {
        memberService.checkDuplicateEmail(emailDuplicationDto.getEmail());
        return BaseResponse.res(SuccessStatus.USER_EMAIL_DUPLICATION,null);
    }

    // 이메일 인증
    @PostMapping("/email_verification")
    public BaseResponse emailVerification(@RequestBody EmailRequestDto emailRequestDto) {
        memberService.validateEmail(emailRequestDto);
        return BaseResponse.res(SuccessStatus.USER_EMAIL_VERIFICATION,null);
    }

    @GetMapping("/agree")
    public void showTermsPage(HttpServletResponse response) throws IOException {
        response.sendRedirect("/terms.html");
    }

    // 약관동의 - 여기 형식도 통일해야할듯
    @PostMapping("/agree")
    public BaseResponse<MemberLoginResponseDto> agree(HttpServletRequest request) {
        return memberService.agreeNewMember();
    }

    // 비밀번호 초기화 기능
    @PostMapping("/reset_pw")
    public BaseResponse<String> passwordFinding(@RequestBody AuthResetRequestDto authResetRequestDto) {
        return memberService.resetPassword(authResetRequestDto);
    }

    // 구글로그인
    @GetMapping("/login/google")
    public BaseResponse<MemberLoginResponseDto> googleLogin
        (@RequestParam String code, HttpServletResponse response) throws IOException {
        if (code == null || code.isEmpty()) {
            return new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), "인가코드가 없습니다.", null);
        }
        return memberService.handleGoogleLogin(code, response);
    }

    // 일반로그인
    @PostMapping("/login")
    public BaseResponse<MemberLoginResponseDto> login(@RequestBody MemberLoginRequestDto memberLoginRequestDto) {
        return BaseResponse.res(SuccessStatus.USER_LOGIN, memberService.userLogin(memberLoginRequestDto));
    }

    // 로그아웃
    @DeleteMapping("/logout")
    public BaseResponse logout(HttpServletRequest request) {
        return memberService.userLogout(request);
    }

    //회원 탈퇴
    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/withdraw")
    public BaseResponse withdraw(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        ObjectId memberId = principalDetails.getId();
        System.out.println(memberId);
        return memberService.userWithdraw(memberId);
    }

    //비밀번호 변경
    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/change_pw")
    public BaseResponse changePw(@AuthenticationPrincipal PrincipalDetails principalDetails) {

    }

}
