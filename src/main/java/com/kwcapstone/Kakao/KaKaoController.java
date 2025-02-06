package com.kwcapstone.Kakao;

import com.kwcapstone.Common.BaseErrorResponse;
import com.kwcapstone.Common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
public class KaKaoController {
    private final KakaoService kakaoService;

    //kakao Login
    @Operation(summary = "카카오 로그인")
    @GetMapping("/auth/login/kakao")
    public BaseResponse<KakaoResponse.KakaoLoginResponse> kakaoLogion(
            @RequestParam String code){
        //code = 인가 코드
        try{
            return BaseResponse()
        }catch(NoSuchElementException e){
            return new BaseErrorResponse(e);
        }
    }
}
