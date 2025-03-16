package com.kwcapstone.Naver.Controller;

import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Common.code.SuccessStatus;
import com.kwcapstone.Naver.Dto.NaverResponse;
import com.kwcapstone.Naver.Service.NaverService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/login")
public class NaverController {
    private final NaverService naverService;

    @Operation(summary = "네이버 로그인 Api")
    @GetMapping("/naver")
    public BaseResponse<NaverResponse.NaverLoginResponse> naverLogin(@RequestParam("code")String code){
        return BaseResponse.res(SuccessStatus.USER_NAVER_LOGIN, naverService.naverLogin(code));
    }
}
