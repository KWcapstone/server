package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendProjectResponseDto {
    private String event;
    private String projectId;  // 이거 그냥 추출해도되려나???
    private SaveScriptDto scription;
}
