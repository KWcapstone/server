package com.kwcapstone.Security;

import com.kwcapstone.Domain.Entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

//Spring Security 에서 사용자 인증과 권한을 관리할 수 있도록 설정
@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {
    //Member 객체를 저장하기 위해 불러옴
    private final Member member;

    //Security 의 사용자 권한 설정 -> 요청마다 사용자 권한 검사
    //Role_ADMIN = 관리자 , ROLE_USER = 사용쟈
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //권한 하나만 추가
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    //사용자 비밀번호 반환
    @Override
    public String getPassword() {
        return null; //비번 기반 인증은 사용하지 않으므로 null
    }

    //사용자 이름 반환
    @Override
    public  String getUsername() {
        return member.getName();
    }

    //계정 상태 관련 메서드
    @Override
    public boolean isAccountNonExpired() {
        return true;//게정 만료 기능을 사용하지 않도록 함
    }

    //계정 잠김 관련 메서드
    @Override
    public boolean isAccountNonLocked() {
        return true; //계정 잠금 기능을 사용하지 않음
    }

    //비밀번호 만료 기능 관련 메서드
    @Override
    public boolean isCredentialsNonExpired() {
        return true; //사용자의 비밀번호 만료 기능을 사용하지 않음
    }

    //계정 활성화 되어있는 지 확인
    @Override
    public boolean isEnabled() {
        return true; //모든 사용자가 활성화 상태
    }

    //사용자의  social Id 추가
    public String getSocialId() {
        return member.getSocialId();
    }
}
