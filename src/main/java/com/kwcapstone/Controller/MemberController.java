package com.kwcapstone.Controller;

import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Domain.Dto.Request.AuthFindRequestDto;
import com.kwcapstone.Domain.Dto.Request.EmailDuplicationDto;
import com.kwcapstone.Domain.Dto.Request.EmailRequestDto;
import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.GoogleLogin.Auth.SessionUser;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {
    @Autowired
    private MemberRepository memberRepository;

    private final MemberService memberService;
    @Autowired
    private HttpSession httpSession;

    // 회원가입
    @PostMapping("/auth/sign_up")
    public BaseResponse signUp(@RequestBody MemberRequestDto memberRequestDto) {
        memberService.join(memberRequestDto);
        return new BaseResponse(HttpStatus.OK.value(), "회원가입이 완료되었습니다.");
    }

    // 이메일 중복 인증
    @PostMapping("/auth/email_duplication")
    public BaseResponse emailDuplication(@RequestBody EmailDuplicationDto emailDuplicationDto) {
        memberService.checkDuplicateEmail(emailDuplicationDto.getEmail());
        return new BaseResponse(HttpStatus.OK.value(), "사용 가능한 이메일");
    }

    // 이메일 인증
    @PostMapping("/auth/email_verification")
    public BaseResponse emailVerification(@RequestBody EmailRequestDto emailRequestDto) {
        memberService.validateEmail(emailRequestDto);
        return new BaseResponse(HttpStatus.OK.value(), "이메일 인증이 완료되었습니다.");
    }

    // 약관동의
    @PostMapping("/auth/agree")
    public BaseResponse agree() {
        Member tempMember = (Member) httpSession.getAttribute("tempMember");

        if (tempMember == null) {
            return new BaseResponse(HttpStatus.BAD_REQUEST.value(), "임시 회원 정보가 없습니다.");
        }

        // 약관 동의 처리 후, DB에 저장
        tempMember.setAgreement(true);
        memberRepository.save(tempMember);

        // 세션 삭제
        httpSession.setAttribute("member", new SessionUser(tempMember));
        httpSession.removeAttribute("tempMember");

        return new BaseResponse(HttpStatus.OK.value(), "회원가입이 완료되었습니다.");
    }

    // 비밀번호 찾기 기능
    @PostMapping("/auth/find_pw")
    public BaseResponse<String> passwordFinding(@RequestBody AuthFindRequestDto authFindRequestDto) {
        return memberService.findPassword(authFindRequestDto);
    }
}
