package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewProjectResponseDto {
    private ObjectId projectId;
    private String projectName;
    private String projectImage;
    private LocalDateTime updatedAt;
    private ObjectId creator;
}
