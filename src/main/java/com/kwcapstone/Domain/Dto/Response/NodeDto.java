package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeDto {
    private String id;
    private String type;
    private List<DataDto> data;
    private List<PositionDto> position;
    private String parentId;
}
