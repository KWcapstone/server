package com.kwcapstone.Kakao.Controller;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Kakao.Dto.KakaoResponse;
import com.kwcapstone.Kakao.Service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
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
        return BaseResponse.res(SuccessStatus.USER_KAKAO_LOGIN,kakaoService.kakaoLogin(code));
    }
}
