package com.kwcapstone.Domain.Entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "invite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invite {
    @Id
    private ObjectId inviteId;

    private String inviteCode;
    private ObjectId projectId;
    private ObjectId userId;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
}
