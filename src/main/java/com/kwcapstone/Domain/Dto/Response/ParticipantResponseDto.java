package com.kwcapstone.Domain.Dto.Response;

import com.kwcapstone.Domain.Dto.Request.ParticipantDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantResponseDto {
    private String event;
    private String projectId;
    private String count;
    private List<ParticipantDto> participants;
}
