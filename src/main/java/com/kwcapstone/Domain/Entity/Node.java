package com.kwcapstone.Domain.Entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "node")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Node {
    @Id
    private ObjectId nodeId;

    private ObjectId projectId;
    private List<Map<String,Object>> nodes;
    private List<Map<String, Object>> edges;

}
