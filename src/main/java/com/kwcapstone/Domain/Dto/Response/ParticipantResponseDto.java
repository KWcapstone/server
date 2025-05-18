package com.kwcapstone.Domain.Dto.Response;

import com.kwcapstone.Domain.Dto.Request.ParticipantRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantResponseDto {
    private String event = "participants";
    private String projectId;
    private List<ParticipantRequestDto> participants;
}
