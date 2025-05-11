package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShowRecordResponseDto {
    private String recordId;
    private String name;
    private LocalDateTime updatedAt;
    private long length;
    private long sizeInBytes;
    private String creator;
}
