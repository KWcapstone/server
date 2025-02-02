package com.kwcapstone.Controller;

import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Service.MemberService;
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

    // 회원가입
    @PostMapping("/auth/sign_up")
    public BaseResponse signUp(@RequestBody MemberRequestDto memberRequestDto) {
        memberService.join(memberRequestDto);
        return new BaseResponse(HttpStatus.OK.value(), "회원가입이 완료되었습니다.");
    }
}
