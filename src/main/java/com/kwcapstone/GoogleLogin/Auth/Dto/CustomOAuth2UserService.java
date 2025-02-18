package com.kwcapstone.GoogleLogin.Auth.Dto;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import com.kwcapstone.GoogleLogin.Auth.OAuthAttributes;
import com.kwcapstone.GoogleLogin.Auth.SessionUser;
import com.kwcapstone.Repository.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final MemberRepository memberRepository;
    private final HttpSession httpSession;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,
                oAuth2User.getAttributes());

        // 기존 회원 찾기
        Member existingMember = memberRepository.findByEmail(attributes.getEmail()).orElse(null);

        if (existingMember != null) {
            // 기존 회원이면 role과 agreement 상태를 유지하면서 정보 업데이트
            existingMember.update(attributes.getName(), attributes.getPicture());
            memberRepository.save(existingMember);
            httpSession.setAttribute("member", new SessionUser(existingMember));
            return createOAuth2User(existingMember, attributes);
        }

        // 새로운 유저 -> 회원가입 보류 (약관 동의 필요)
        Member tempMember = attributes.toEntity();
        tempMember.setRole(getRoleFromRegistrationId(registrationId));
        tempMember.setAgreement(false);  // 기본값: false
        httpSession.setAttribute("tempMember", tempMember);  // 임시로 세션에 저장 (DB 저장 X)

        // 약관 동의 페이지로 리디렉트
        try {
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes()).getResponse();
            if (response != null) {
                response.sendRedirect("/terms.html");
            }
        } catch (IOException e) {
            throw new RuntimeException("약관 동의 페이지로 이동 중 오류 발생", e);
        }

        return createOAuth2User(tempMember, attributes);
    }

    private OAuth2User createOAuth2User(Member member, OAuthAttributes attributes) {
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private Member saveOrUpdate(OAuthAttributes attributes, String registrationId) {
        MemberRole role = getRoleFromRegistrationId(registrationId);

        Member member = memberRepository.findByEmail(attributes.getEmail())
                .map(entity -> {
                    entity.update(attributes.getName(), attributes.getPicture());
                    if (entity.getRole() == null) {
                        entity.setRole(role);
                    }
                    return entity;
                })
                .orElseGet(() -> {
                    Member newMember = attributes.toEntity();
                    newMember.setRole(role);
                    return newMember;
                });
        // 기존 유저라도 role이 null이면 GUEST로 설정

        return memberRepository.save(member);
    }

    private MemberRole getRoleFromRegistrationId(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> MemberRole.GOOGLE;
            case "naver" -> MemberRole.NAVER;
            case "kakao" -> MemberRole.KAKAO;
            default -> MemberRole.USER;
        };
    }
}