package com.kwcapstone.Service;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.kwcapstone.Common.BaseErrorResponse;
import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Common.PasswordGenerator;
import com.kwcapstone.Common.code.SuccessStatus;
import com.kwcapstone.Domain.Dto.Request.AuthResetRequestDto;
import com.kwcapstone.Domain.Dto.Request.EmailRequestDto;
import com.kwcapstone.Domain.Dto.Request.MemberLoginRequestDto;
import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import com.kwcapstone.Domain.Dto.Response.GoogleTokenResponseDto;
import com.kwcapstone.Domain.Dto.Response.MemberLoginResponseDto;
import com.kwcapstone.Domain.Entity.EmailVerification;
import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.Exception.BaseException;
import com.kwcapstone.GoogleLogin.Auth.GoogleUser;
import com.kwcapstone.GoogleLogin.Auth.SessionUser;
import com.kwcapstone.Repository.EmailVerificationRepository;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Domain.Token;
import com.kwcapstone.Token.JwtTokenProvider;
import com.kwcapstone.Token.Repository.TokenRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    private final HttpSession httpSession;
    private final Filter springSecurityFilterChain;

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

    // 비밀번호 초기화
    public BaseResponse<String> resetPassword(AuthResetRequestDto authResetRequestDto) {
        Optional<Member> memberExist = memberRepository.findByNameAndEmail(
                authResetRequestDto.getName(), authResetRequestDto.getEmail());

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
            return BaseResponse.res(SuccessStatus.USER_RESET_PW,null);
            //return new BaseResponse<>(HttpStatus.OK.value(), "이메일에 발송된 비밀번호를 확인하세요.");
        } else if (role == MemberRole.GOOGLE || role == MemberRole.NAVER || role == MemberRole.KAKAO) {
            return BaseResponse.res(SuccessStatus.USER_AlREADY_SOCIAL_LOGIN,null);
            //return new BaseResponse<>(HttpStatus.OK.value(), "소셜 로그인으로 가입된 이메일입니다. 일반 로그인이 아닌 소셜 로그인을 사용해 주세요.");
        }
        return new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), "잘못된 요청입니다.");
    }

    // 구글 로그인
    public BaseResponse<MemberLoginResponseDto> handleGoogleLogin
        (String authorizationCode, HttpServletResponse response) throws IOException {
        // jwt를 위한 코드를 받으러감
        BaseResponse<String> tokenResponse = googleOAuthService.getAccessToken(authorizationCode);

        // 실제 accessToken 값 꺼내기
        if (tokenResponse.getStatus() != HttpStatus.OK.value()) {
            return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Google OAuth 오류 : " + tokenResponse.getMessage(), null);
        }

        String googleAccessToken = tokenResponse.getData();
        BaseResponse<GoogleUser> userResponse = googleOAuthService.getUserInfo(googleAccessToken);

        if (userResponse.getStatus() != HttpStatus.OK.value()) {
            return new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Google 사용자 정보 요청 오류: " + userResponse.getMessage(), null);
        }

        GoogleUser googleUser = userResponse.getData();
        Member member = memberRepository.findByEmail(googleUser.getEmail()).orElse(null);

        Map<String, String> tokens;
        MemberLoginResponseDto tokenResponseDto;

        // 새로운 멤버인 경우 저장
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

            httpSession.setAttribute("tempMember", member);

            response.sendRedirect("/auth/agree");
            return null;
        }
        httpSession.setAttribute("member", new SessionUser(member));
        // jwt 사용할 것
        tokenResponseDto = getMemberToken(member);

        return BaseResponse.res(SuccessStatus.USER_GOOGLE_LOGIN,null);
        //return new BaseResponse<>(HttpStatus.OK.value(), "로그인 성공", tokenResponseDto);
    }

    private MemberLoginResponseDto getMemberToken(Member member) {
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getMemberId(), member.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId(), member.getRole().name());

        Optional<Token> present = tokenRepository.findByMemberId(member.getMemberId());

        if (present.isPresent()) {
            present.get().changeToken(newAccessToken, newRefreshToken);
        } else {
            tokenRepository.save(new Token(newAccessToken, newRefreshToken, member.getMemberId()));
            memberRepository.save(member);
        }
        return new MemberLoginResponseDto(member.getMemberId(), newAccessToken);
    }

    // 약관 동의 (새로운 Google User)
    public BaseResponse<MemberLoginResponseDto> agreeNewMember() {
        Member tempMember = (Member) httpSession.getAttribute("tempMember");

        if (tempMember == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "임시 회원 정보가 없습니다.");
        }

        // 약관 동의 처리 후, DB에 저장
        tempMember.setAgreement(true);
        memberRepository.save(tempMember);

        MemberLoginResponseDto tokenResponseDto = getMemberToken(tempMember);

        httpSession.setAttribute("tempMember", new SessionUser(tempMember));
        httpSession.removeAttribute("tempMember");

        return BaseResponse.res(SuccessStatus.USER_NEW_GOOGLE_LOGIN,null);
        //return new BaseResponse<>(HttpStatus.OK.value(), "로그인 성공", tokenResponseDto);
    }

    // 일반 유저 로그인
    public MemberLoginResponseDto userLogin(MemberLoginRequestDto memberLoginRequestDto) {
        Optional<Member> member = memberRepository.findByEmail(memberLoginRequestDto.getEmail());
        if (member.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 아이디를 찾을 수 없습니다.");
        }
        if (!memberLoginRequestDto.getPassword().equals(member.get().getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 확인해주세요.");
        }

        return getMemberToken(member.get());
    }

    public BaseResponse userLogout(HttpServletRequest request) {
        String accessToken;
        // Access Token 추출
        try {
            accessToken = jwtTokenProvider.extractToken(request);
        } catch (ResponseStatusException e) {
            return new BaseErrorResponse(HttpStatus.UNAUTHORIZED.value(), "유효하지 않은 Access Token 입니다.");
        }

        // Access Token 유효성 검사
        try {
            if (!jwtTokenProvider.isTokenValid(accessToken)) {
                return new BaseErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                        "만료되었거나 유효하지 않은 Access Token입니다.");
            }
        } catch (ResponseStatusException e) {
            return new BaseErrorResponse(HttpStatus.UNAUTHORIZED.value(), e.getReason());
        }

        // Access Token에서 사용자 ID 추출
        String userIdStr;
        try {
            userIdStr = jwtTokenProvider.getId(accessToken);
        } catch (Exception e) {
            return new BaseErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                    "Access Token에서 사용자 정보를 추출할 수 없습니다.");
        }

        // 여기 추가함
        if (!ObjectId.isValid(userIdStr)) {
            return new BaseErrorResponse(HttpStatus.BAD_REQUEST.value(), "잘못된 사용자 ID 형식입니다.");
        }

        // String -> ObjectId 변환
        ObjectId userId;
        try {
            userId = new ObjectId(userIdStr);
            System.out.println("ObjectId 변환 완료: " + userId);
        } catch (IllegalArgumentException e) {
            return new BaseErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    "잘못된 사용자 ID 형식입니다.");
        }

        // db에서 해당 사용자의 refresh token 삭제
        tokenRepository.deleteById(userId);

        // 로그아웃 완료 응답 반환
        return BaseResponse.res(SuccessStatus.USER_LOGOUT,null);
        //return new BaseResponse(HttpStatus.OK.value(), "로그아웃이 완료되었습니다.");
    }
}
