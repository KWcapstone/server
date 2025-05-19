package com.kwcapstone.Domain.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {
    private String memberId;
    private String memberName;
    private String profileImage;

    public ParticipantDto(String memberId) {
        this.memberId = memberId;
    }
}