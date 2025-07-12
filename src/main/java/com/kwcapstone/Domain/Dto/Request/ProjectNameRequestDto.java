package com.kwcapstone.Domain.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectNameRequestDto {
    private String event;
    private String projectId;
    private String projectName;
}
