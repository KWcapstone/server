package com.kwcapstone.Service;

import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Common.PasswordGenerator;
import com.kwcapstone.Domain.Dto.Request.AuthFindRequestDto;
import com.kwcapstone.Domain.Dto.Request.EmailRequestDto;
import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import com.kwcapstone.Domain.Entity.EmailVerification;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.Exception.BaseException;
import com.kwcapstone.GoogleLogin.Auth.GoogleUser;
import com.kwcapstone.Repository.EmailVerificationRepository;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRepository tokenRepository;

    private final EmailService emailService;
    private final GoogleOAuthService googleOAuthService;

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
            throw new BaseException(422, "비밀번호는 6자 이상 12자 이하이며, " +
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

    // 비밀번호 찾기
    public BaseResponse<String> findPassword(AuthFindRequestDto authFindRequestDto) {
        Optional<Member> memberExist = memberRepository.findByNameAndEmail(
                authFindRequestDto.getName(), authFindRequestDto.getEmail());

        if (memberExist.isEmpty()) {
            throw new BaseException(404, "가입하지 않은 회원입니다. 이름이나 이메일을 다시 확인해주세요.");
        }

        Member member = memberExist.get();
        MemberRole role = member.getRole();

        if (role == MemberRole.USER) {
            String newPassword = PasswordGenerator.generateRandomPassword();

            member.setPassword(newPassword);
            memberRepository.save(member);

            // 이메일 발송
            emailService.sendPasswordResetMessage(member.getEmail(), newPassword);
            return new BaseResponse<>(HttpStatus.OK.value(), "이메일에 발송된 비밀번호를 확인하세요.");
        } else if (role == MemberRole.GOOGLE || role == MemberRole.NAVER || role == MemberRole.KAKAO) {
            return new BaseResponse<>(HttpStatus.OK.value(),
                    "소셜 로그인으로 가입된 이메일입니다. 일반 로그인이 아닌 소셜 로그인을 사용해 주세요.");
        }
        return new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), "잘못된 요청입니다.");
    }

    // 구글 로그인
    public BaseResponse<Map<String, String>> handleGoogleLogin(String authorizationCode) {
        BaseResponse<String> response = googleOAuthService.getAccessToken(authorizationCode);

        // 실제 accessToken 값 꺼내기
        if (response.getStatus() != HttpStatus.OK.value()) {
            return new BaseResponse<>(response.getStatus(),
                    "Google OAuth 오류" + response.getMessage(), new HashMap<>());
        }

        String googleAccessToken = response.getData();
        BaseResponse<GoogleUser> userResponse = googleOAuthService.getUserInfo(googleAccessToken);

        if (userResponse.getStatus() != HttpStatus.OK.value()) {
            return new BaseResponse<>(userResponse.getStatus(),
                    "Google 사용자 정보 요청 오류: " + userResponse.getMessage(), new HashMap<>());
        }

        GoogleUser googleUser = userResponse.getData();
        Map<String, String> tokens = processGoogleUser(googleUser);

        return new BaseResponse<>(HttpStatus.OK.value(), "로그인 성공", tokens);
    }

    public Map<String, String> processGoogleUser(GoogleUser googleUser) {
        Member member = memberRepository.findByEmail(googleUser.getEmail()).orElse(null);

        if (member == null) {
            member = Member.builder()
                    .socialId(googleUser.getSocialId())
                    .name(googleUser.getName())
                    .email(googleUser.getEmail())
                    .image(googleUser.getPicture())
                    .role(MemberRole.GOOGLE)
                    .agreement(false)
                    .build();
            memberRepository.save(member);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getSocialId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getSocialId(), member.getRole().name());

        Token token = new Token(accessToken, refreshToken, member.getMemberId());
        tokenRepository.save(token);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }
}
