package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponseWrapperDto {
    private String tap;  // entire, record, summary
    private String projectId;
    private String projectName;
    private LocalDateTime updatedAt;
    private String creator;
    private Object result;  // 각 탭별 DTO

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EntireDto {
        private String imageUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ZipDto {
        private long documentFileSize;
        private long recordFileSize;
        private String fileName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SummaryDto {
        private long sizeInBytes;
        private String fileName;
    }
}
