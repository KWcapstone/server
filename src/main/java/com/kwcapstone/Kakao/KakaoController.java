package com.kwcapstone.Kakao;

import com.kwcapstone.Common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/login")
public class KakaoController {
    private final KakaoService kakaoService;

    @Operation(summary = "카카오 로그인 Api")
    @GetMapping("/kakao")
    public BaseResponse<KakaoResponse.KakaoLoginResponse> kakaoLogin(@RequestParam("code") String code){
        return new BaseResponse (HttpStatus.OK.value(),"로그인이 성공적으로 완료되었습니다.", kakaoService.kakaoLogin(code));
    }
}
