package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NodeUpdateResponseDto {
    private String event;
    private String projectId;
    private NodeSummaryResponseDto summary;
    private List<NodeDto> nodes;
}
