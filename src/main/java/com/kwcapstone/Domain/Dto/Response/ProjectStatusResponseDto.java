package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStatusResponseDto {
    private String projectId;
    private String creator;
    private String status;
    private String projectName;
    private LocalDateTime updatedAt;
}