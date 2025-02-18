package com.kwcapstone.Domain.Entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
    GOOGLE("ROLE_GOOGLE", "Google User"),
    NAVER("ROLE_NAVER", "Naver User"),
    KAKAO("ROLE_KAKAO", "Kakao User"),
    USER("ROLE_USER", "Common User");

    private final String key;
    private final String title;
}