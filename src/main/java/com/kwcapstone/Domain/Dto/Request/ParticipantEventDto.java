package com.kwcapstone.Domain.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantEventDto {
    private String event;  // "participant_join" or "participant_leave"
    private String projectId;
    private ParticipantDto participant;
}
