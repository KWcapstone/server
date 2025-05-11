package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectNameEditResponseDto {
    private String projectId;
    private String projectName;
}
