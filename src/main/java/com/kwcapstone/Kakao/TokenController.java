package com.kwcapstone.Kakao;

import com.kwcapstone.Common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class TokenController {
    private final TokenService tokenService;

    @Operation(
            summary = "JWT ACCESS TOKEN 재발급 API")
    @PostMapping("/refresh")
    public BaseResponse reissueToken(final HttpServletRequest request) {
        TokenResponse tokenResponse = tokenService.reissueToken(request);

        return new BaseResponse(HttpStatus.OK.value(), "토큰 재발급이 완료되었습니다.");
    }
}
