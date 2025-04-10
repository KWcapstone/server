package com.kwcapstone.Domain.Entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "memberToProject")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberToProject {
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId mappingId;

    @Indexed
    private ObjectId projectId;

    @Indexed
    private ObjectId memberId;
}
