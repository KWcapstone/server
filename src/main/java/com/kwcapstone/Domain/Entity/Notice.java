package com.kwcapstone.Domain.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notice {
    @Id
    private ObjectId noticeId;
    private String title;
    private String content;
    private LocalDateTime createAt;
    private Boolean isRead;
    private ObjectId userId;
    private ObjectId senderId;
}
