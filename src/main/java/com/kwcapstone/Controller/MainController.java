package com.kwcapstone.Controller;

import com.kwcapstone.Common.BaseResponse;
import com.kwcapstone.Common.code.SuccessStatus;
import com.kwcapstone.Service.MainService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main")
public class MainController {
    private final MainService mainService;

    @GetMapping("/notice/{memberId}")
    public BaseResponse noticeShow (@PathVariable("memberId") String memberId,
                                    @RequestParam(value = "type", defaultValue = "all") String type) {
        return BaseResponse.res(SuccessStatus.NOTICE_CONFIRM, mainService.showNotice(memberId, type));
    }
}
