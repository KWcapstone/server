package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDto {
    private String memberId;
    private String name;
    private String email;
    private String imageUri;
}
