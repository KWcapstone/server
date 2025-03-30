package com.kwcapstone.Domain.Dto.Request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PasswordRequestDto {
    private String originalPassword;
    private String changePassword;
}
