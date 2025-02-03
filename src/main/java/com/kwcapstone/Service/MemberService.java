package com.kwcapstone.Service;

import com.kwcapstone.Domain.Dto.Request.EmailRequestDto;
import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import com.kwcapstone.Domain.Entity.EmailVerification;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Exception.BadRequestException;
import com.kwcapstone.Exception.BaseException;
import com.kwcapstone.Repository.EmailVerificationRepository;
import com.kwcapstone.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    private final EmailService emailService;

    // 회원가입
    @Transactional
    public void join(MemberRequestDto memberRequestDto) {
        validateAuthRequest(memberRequestDto);
        EmailVerification emailVerification = emailVerificationRepository
                .findLatestByEmail(memberRequestDto.getEmail())
                .orElseThrow(() -> new BaseException(400, "이메일 인증이 필요합니다."));

        if (!emailVerification.isVerified()) {
            throw new BaseException(400, "이메일 인증을 완료해야 회원가입이 가능합니다.");
        }
        memberRepository.save(convertToMember(memberRequestDto));
    }

    String namePattern = "^[가-힣A-Za-z]{2,15}$";
    String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,12}$";
    private void validateAuthRequest(MemberRequestDto memberRequestDto) {
        if (memberRequestDto.getName() == null) {
            throw new BaseException(400, "이름을 입력해주세요.");
        }
        if (!Pattern.matches(namePattern, memberRequestDto.getName())) {
            throw new BaseException(422, "이름은 한글 또는 영어만 입력할 수 있으며, 2자 이상 15자 이하만 입력해야 합니다.");
        }
        if (memberRequestDto.getEmail() == null) {
            throw new BaseException(400, "이메일을 입력해주세요.");
        }
        if (memberRequestDto.getPassword() == null) {
            throw new BaseException(400, "비밀번호를 입력해주세요.");
        }
        if (!Pattern.matches(passwordPattern, memberRequestDto.getPassword())) {
            throw new BaseException(422, "비밀번호를 6자 이상 12자 이하이며, " +
                    "영문자, 숫자, 특수문자(@$!%*?&)를 각각 최소 1개 이상 포함해야 합니다.");
        }
        if (!memberRequestDto.isAgreement()) {
            throw new BaseException(400, "약관에 동의해주세요.");
        }
    }

    private Member convertToMember(MemberRequestDto memberRequestDto) {
        return new Member(memberRequestDto);
    }

    // 이메일 중복 체크
    public void checkDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BaseException(400, "이미 가입된 이메일입니다.");
        }
        if (!Pattern.matches(emailPattern, email)) {
            throw new BaseException(422, "이메일 형식이 올바르지 않습니다. @를 포함한 올바른 이메일을 입력해주세요.");
        }
        requestEmailVerification(email);  // 확인 이메일 전송
    }

    // 이메일 인증
    public void validateEmail(EmailRequestDto emailRequestDto) {
        EmailVerification emailVerification = emailVerificationRepository
                .findLatestByEmail(emailRequestDto.getEmail())
                .orElseThrow(() -> new BaseException(400, "이메일 인증이 필요합니다."));

        if (emailVerification.getExpirationTime().isBefore(LocalDateTime.now())) {
            throw new BaseException(400, "인증 번호가 만료되었습니다.");
        }

        if (emailVerification.getVerificationCode() == null ||
            !emailVerification.getVerificationCode().equals(emailRequestDto.getCode())) {
            throw new BaseException(400, "인증 번호가 일치하지 않습니다.");
        }
        emailVerification.setVerified(true);
        emailVerificationRepository.save(emailVerification);
    }

    // 이메일 인증 코드 생성 및 저장
    public void requestEmailVerification(String email) {
        Integer verificationCode = (int)(Math.random()*1000000);
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(10);  // 유효 시간 10분
        EmailVerification emailVerification = new EmailVerification(email, verificationCode, expirationTime);
        emailVerificationRepository.save(emailVerification);

        emailService.sendEmailRequestMessage(email, verificationCode.toString());
    }
}
