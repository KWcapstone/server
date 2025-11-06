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
    private long length; //음성길이
    private long sizeInBytes; //문서
    private String creator;
}
