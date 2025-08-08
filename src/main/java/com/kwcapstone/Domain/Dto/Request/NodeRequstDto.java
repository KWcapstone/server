package com.kwcapstone.Domain.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeRequstDto {
    private String event;
    private String projectId;
    private String nodes; //2. liveOff 일 때 수정된 노드가 옴
}