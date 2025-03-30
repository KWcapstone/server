package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleTokenResponseDto {
    private ObjectId memberId;
    private String accessToken;
    private String refreshToken;
}
