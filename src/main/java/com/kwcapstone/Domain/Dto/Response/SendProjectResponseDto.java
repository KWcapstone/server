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
    private String projectId;
    private SaveScriptDto scription;  // 단일용
    private List<SaveScriptDto> scriptList;   // 히스토리용

    public SendProjectResponseDto(String event, String projectId, SaveScriptDto scription) {
        this.event = event;
        this.projectId = projectId;
        this.scription = scription;
    }

    public SendProjectResponseDto(String event, String projectId, List<SaveScriptDto> scriptList) {
        this.event = event;
        this.projectId = projectId;
        this.scriptList = scriptList;
    }
}
