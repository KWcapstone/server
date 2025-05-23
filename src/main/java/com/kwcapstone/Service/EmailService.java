package com.kwcapstone.Service;

import com.kwcapstone.Exception.BaseException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;

@PropertySource("classpath:application.properties")
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String name;

    public void sendEmailRequestMessage(String to, String code) {
        try {
            javaMailSender.send(createEmailRequestMessage(to, code.toString()));
        } catch (MessagingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 전송 실패: 메시지 생성 중 오류 발생");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송 실패");
        }
    }

    private MimeMessage createEmailRequestMessage(String to, String code)
            throws MessagingException, UnsupportedEncodingException {
        log.info("보내는 대상 : " + to);
        log.info("인증 번호 : " + code);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        mimeMessage.addRecipients(MimeMessage.RecipientType.TO, to);
        mimeMessage.setSubject("MoAba 회원가입 인증 코드 메일");

        // 메일 내용
        String message = "";
        message += "<h3>아래 확인 코드를 회원가입 화면에서 입력해주세요.</h3>";
        message += "<h1>" + code + "</h1>";
        message += "<h3>감사합니다.</h3>";
        mimeMessage.setText(message, "utf-8", "html");
        mimeMessage.setFrom(new InternetAddress(name, "MoAba"));

        return mimeMessage;
    }

    public void sendPasswordResetMessage(String to, String newPassword) {
        try {
            javaMailSender.send(sendPasswordResetEmail(to, newPassword));
        } catch (MessagingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 전송 실패: 메시지 생성 중 오류 발생");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송을 실패하였습니다.");
        }
    }

    public MimeMessage sendPasswordResetEmail(String to, String newPassword)
        throws MessagingException, UnsupportedEncodingException {
        log.info("비밀번호 재설정 이메일 대상 : " + to);
        log.info("새로운 비밀번호 : " + newPassword);  // 여기 보안상 손봐야할지도..??

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        mimeMessage.addRecipients(MimeMessage.RecipientType.TO, to);
        mimeMessage.setSubject("MoAba 비밀번호 재설정 안내");

        // 이메일 내용
        String message = "";
        message += "<h3>아래의 새로운 비밀번호를 사용하여 로그인하세요</h3>";
        message += "<h1>" + newPassword + "</h1>";
        message += "<p>로그인 후 반드시 비밀번호를 변경해주세요.</p>";
        message += "<h3>감사합니다.</h3>";

        mimeMessage.setText(message, "utf-8", "html");
        mimeMessage.setFrom(new InternetAddress(name, "MoAba"));

        return mimeMessage;
    }

    public void sendProjectInviteMessage(String to, String inviteLink, String inviterName, String projectName) {
        try {
            javaMailSender.send(createProjectInviteMessage(to, inviteLink, inviterName, projectName));
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트 초대 이메일 전송 실패");
        }
    }

    private MimeMessage createProjectInviteMessage(String to, String inviteLink, String inviterName, String projectName)
        throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        mimeMessage.addRecipients(MimeMessage.RecipientType.TO, to);
        mimeMessage.setSubject("MoAba 프로젝트 초대 메일");

        // 어떤 유저가 초대한 + 프로젝트 이름 까지 함께 보내야할듯?
        String message = "<h3>" + inviterName + "님이 " + projectName + " 프로젝트에 초대하셨습니다!</h3>"
                + "<p>아래 버튼을 눌러 프로젝트에 참여해보세요.</p>"
                + "<a href=\"" + inviteLink + "\">[프로젝트 참여하기]</a>";

        mimeMessage.setText(message, "utf-8", "html");
        mimeMessage.setFrom(new InternetAddress(name, "MoAba"));

        return mimeMessage;
    }
}
