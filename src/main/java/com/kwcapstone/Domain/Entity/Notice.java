package com.kwcapstone.Domain.Entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {
    @Id
    private ObjectId noticeId;
    private String title;
    private String url;
    private LocalDateTime createAt;
    private Boolean official;
    private Boolean isRead;
    private ObjectId userId;
    private ObjectId senderId;
}
