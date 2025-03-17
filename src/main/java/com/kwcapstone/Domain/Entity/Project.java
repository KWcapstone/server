package com.kwcapstone.Domain.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "project")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    @Id
    private ObjectId projectId;
    private String projectName;
    private String projectImage;
    private Record record;
    private Script script;
    private String summary;
    private LocalDateTime updateAt;
    private ObjectId creator;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Record{
        private String fileUrl;
        private String fileName;
        private long length;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Script{
        private String content;
        private String fileUrl;
    }
}
