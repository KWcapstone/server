package com.kwcapstone.GoogleLogin.Auth;

import lombok.Getter;

@Getter
public class GoogleUser {
    private String sub;
    private String email;
    private String name;
    private String picture;

    public String getSocialId() {
        return sub;  // google의 고유 ID를 socialId로 사용
    }
}
