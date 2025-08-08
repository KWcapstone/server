package com.kwcapstone.Domain.Dto.Request;

import com.kwcapstone.Domain.Dto.Response.NodeDto;
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
    private String scription; //1.liveon 일때 실시간 응답
    private String nodes; //2. liveOff 일 때 수정된 노드가 옴
}