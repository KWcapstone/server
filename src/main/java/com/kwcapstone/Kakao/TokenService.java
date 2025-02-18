package com.kwcapstone.Kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional //동시성과 DB를 위함
public class TokenService {
    private final TokenRepository tokenRepository;


}
