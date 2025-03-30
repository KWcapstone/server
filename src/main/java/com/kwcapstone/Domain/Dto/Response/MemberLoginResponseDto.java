package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@NoArgsConstructor
public class MemberLoginResponseDto {
    private String memberId;
    private String accessToken;
    private String refreshToken;

    public MemberLoginResponseDto(ObjectId memberId, String accessToken,String refreshToken) {
        this.memberId = memberId.toHexString();
        this.accessToken=accessToken;
        this.refreshToken=refreshToken;
    }
}
