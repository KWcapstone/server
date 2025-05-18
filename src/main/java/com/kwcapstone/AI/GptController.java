package com.kwcapstone.AI;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/gptTest")
public class GptController {
    private final GptService gptService;

    @Operation(summary = "gpt summary test")
    @PostMapping("/summary")
    public BaseResponse summaryByGPT(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                     @RequestBody String originalText) {
        return BaseResponse.res(SuccessStatus.GPT_SUMMARY_SUCCESS, gptService.callSummaryOpenAI(originalText));
    }
}
