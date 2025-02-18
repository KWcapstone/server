package com.kwcapstone.GoogleLogin.Auth;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.MemberRole;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;
    private String socialId;
    private MemberRole role;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name,
                           String email, String picture, String socialId, MemberRole role) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.socialId = socialId;
        this.role = role;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String,
            Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
        };
    }

    public static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .socialId((String) attributes.get("sub"))
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .role(MemberRole.GOOGLE)
                .build();
    }

    public Member toEntity() {
        return Member.builder()
                .socialId(socialId)
                .name(name)
                .email(email)
                .image(picture)
                .role(role)
                .agreement(false)  // 기본적으로는 약관 동의 전
                .build();
    }
}
