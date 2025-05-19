package com.kwcapstone.AI;

import com.kwcapstone.Common.Response.BaseResponse;
import com.kwcapstone.Common.Response.SuccessStatus;
import com.kwcapstone.Security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gptTest")
public class GptController {
    private final GptService gptService;

    @Operation(summary = "gpt summaryword test")
    @PostMapping("/summary")
    @ResponseBody
    public BaseResponse summaryByGPT(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                     @RequestBody String originalText) {
        return BaseResponse.res(SuccessStatus.GPT_SUMMARY_SUCCESS, gptService.callSummaryOpenAI(originalText));
    }

    @Operation(summary = "gpt recommended keywords test")
    @PostMapping("/recommended_keywords")
    @ResponseBody
    public BaseResponse recommendByGPT(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                       @RequestBody String originalText) {
        return BaseResponse.res(SuccessStatus.GPT_RECOMMEND_SUCCESS, gptService.callRecommendedKeywords(originalText));
    }

    @Operation(summary = "gpt main keyword test")
    @PostMapping("/recommend")
    public BaseResponse mainByGPT(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                      @RequestBody String originalText) {
        return BaseResponse.res(SuccessStatus.GPT_MAIN_SUCCESS, gptService.callMainOpenAI(originalText));
    }

}
