package com.kwcapstone.Domain.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EdgeDto {
    private String id; //3->4
    private String source;
    private String target;
}
