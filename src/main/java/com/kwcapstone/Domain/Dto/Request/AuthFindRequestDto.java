package com.kwcapstone.Domain.Dto.Request;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthFindRequestDto {
    private String name;
    private String email;
}
