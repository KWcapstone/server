package com.kwcapstone.Security;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Repository.MemberRepository;
import com.kwcapstone.Token.Service.TokenService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
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
    private final TokenService tokenService;

    //유저 이름으로
    @Override
    public UserDetails loadUserByUsername(String stringMemberId) throws UsernameNotFoundException {
        ObjectId memberId = tokenService.ConvertToObjectId(stringMemberId);

        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자 입니다."));

        return new PrincipalDetails(member);
    }
}
