package com.kwcapstone.Security;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    //유저 이름으로
    @Override
    public UserDetails loadUserByUsername(String socialId) throws UsernameNotFoundException {
        Member member = memberRepository.findBySocialId(socialId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자 입니다."));

        return new PrincipalDetails(member);
    }
}
