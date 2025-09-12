package com.kwcapstone.Domain.Dto.Response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class getProjectInfoResponseDto {
    private String projectId;
    private String projectName;
    private LocalDateTime updateAt;
    private String imageUrl;
    private String script;
    private JsonNode summary;
}
