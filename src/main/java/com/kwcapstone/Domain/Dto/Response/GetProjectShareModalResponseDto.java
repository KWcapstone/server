package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetProjectShareModalResponseDto {
    private String inviteUrl;
    private List<MemberInfoDto> sharedMembers;
}
