package com.kwcapstone.Domain.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRequestDto {
    private String memberId;
    private String memberName;
    private String profileImage;
}