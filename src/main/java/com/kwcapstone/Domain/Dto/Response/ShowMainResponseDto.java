package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShowMainResponseDto {
    private ObjectId projectId;
    private String projectName;
    private LocalDateTime updatedAt;
    private String creator;
    private String imageUrl;
}
