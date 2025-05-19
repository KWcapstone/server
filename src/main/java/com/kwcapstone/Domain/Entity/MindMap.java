package com.kwcapstone.Domain.Entity;

import com.kwcapstone.Domain.Dto.Response.NodeDto;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "mindMap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindMap {
    @Id
    private ObjectId nodeObjectId;
    private String id;
    private ObjectId projectId;
    private List<NodeDto> nodes;
    private List<Map<String, Object>> edges;

}
