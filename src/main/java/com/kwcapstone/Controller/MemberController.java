package com.kwcapstone.Controller;

import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Domain.Dto.Request.*;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.GoogleLogin.Auth.SessionUser;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        return new BaseResponse(HttpStatus.OK.value(), "회원가입이 완료되었습니다.");
    }

    // 이메일 중복 인증
    @PostMapping("/email_duplication")
    public BaseResponse emailDuplication(@RequestBody EmailDuplicationDto emailDuplicationDto) {
        memberService.checkDuplicateEmail(emailDuplicationDto.getEmail());
        return new BaseResponse(HttpStatus.OK.value(), "사용 가능한 이메일");
    }

    // 이메일 인증
    @PostMapping("/email_verification")
    public BaseResponse emailVerification(@RequestBody EmailRequestDto emailRequestDto) {
        memberService.validateEmail(emailRequestDto);
        return new BaseResponse(HttpStatus.OK.value(), "이메일 인증이 완료되었습니다.");
    }

    @GetMapping("/agree")
    public void showTermsPage(HttpServletResponse response) throws IOException {
        response.sendRedirect("/terms.html");
    }

    // 약관동의 - 여기 형식도 통일해야할듯
    @PostMapping("/agree")
    public ResponseEntity<Map<String, String>> agree(HttpServletResponse response) throws IOException {
        Member tempMember = (Member) httpSession.getAttribute("tempMember");

        // 세션 값 확인용 로그 추가
        System.out.println("tempMember: " + tempMember);

        if (tempMember == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "임시 회원 정보가 없습니다."));
        }

        // 약관 동의 처리 후, DB에 저장
        tempMember.setAgreement(true);
        memberRepository.save(tempMember);

        Map<String, String> tokens = memberService.processGoogleUser(tempMember);

        // 세션 삭제
        httpSession.setAttribute("member", new SessionUser(tempMember));
        httpSession.removeAttribute("tempMember");

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "회원가입이 완료되었습니다.");
        responseBody.put("redirectUrl", "/");

        return ResponseEntity.ok(responseBody);
    }

    // 비밀번호 초기화 기능
    @PostMapping("/reset_pw")
    public BaseResponse<String> passwordFinding(@RequestBody AuthResetRequestDto authResetRequestDto) {
        return memberService.resetPassword(authResetRequestDto);
    }

    // 구글로그인
    @GetMapping("/login/google")
    public BaseResponse<Map<String, String>> googleLogin
        (@RequestParam String code, HttpServletResponse response) throws IOException {
        if (code == null || code.isEmpty()) {
            return new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), "인가코드가 없습니다.", null);
        }
        return memberService.handleGoogleLogin(code, response);
    }

    // 일반로그인
    @PostMapping("/login")
    public BaseResponse<Map<String, String>> login(@RequestBody MemberLoginRequestDto memberLoginRequestDto) {
        return new BaseResponse<>(HttpStatus.OK.value(), "로그인이 완료되었습니다.",
                memberService.userLogin(memberLoginRequestDto));
    }

    // 로그아웃
    @DeleteMapping("/logout")
    public BaseResponse logout(HttpServletRequest request) {
        return memberService.userLogout(request);
    }
}
