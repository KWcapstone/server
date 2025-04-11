package com.kwcapstone.Domain.Dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDeleteRequestDto {
    @Schema(type = "string", description = "MongoDB ObjectId 형식의 프로젝트 ID")
    private ObjectId projectId;
    private String type;
}
