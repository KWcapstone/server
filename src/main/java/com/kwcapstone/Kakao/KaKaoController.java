package com.kwcapstone.Kakao;

import com.kwcapstone.Common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KaKaoController {
    private final KakaoService kakaoService;

    @Operation(summary = "카카오 로그인")
    @GetMapping("/auth/login/kakao")
    public BaseResponse<>
}
