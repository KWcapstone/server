package com.kwcapstone.Domain.Entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.kwcapstone.Domain.Dto.Response.SaveScriptDto;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {
    @Id
    private ObjectId projectId;
    private String projectName;
    private String projectImage;
    private Record record;
    private Script script;
    private Summary summary;
    private LocalDateTime updatedAt;
    private ObjectId creator;
    private String status;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Record {
        private String fileUrl;
        private String fileName;
        private long fileSize;
        private String length;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Script {
        private String scriptUrl;
        private List<SaveScriptDto> scriptions;
        private long sizeInBytes;  // 스크립트 파일 크기 (바이트 단위)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private String summaryUrl;
        private String content;
        private long sizeInBytes;
    }

    public void editName(String newName){
        this.projectName = newName;
    }
}
