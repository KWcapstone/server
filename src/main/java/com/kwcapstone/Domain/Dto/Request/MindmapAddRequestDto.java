package com.kwcapstone.Domain.Dto.Request;

import com.kwcapstone.Domain.Dto.Response.PositionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MindmapAddRequestDto {
    private String event;
    private String projectId;
    public class node {
        private String nodeId;
        private String content;
        private PositionDto position;
        private String partendId;
    }
}
