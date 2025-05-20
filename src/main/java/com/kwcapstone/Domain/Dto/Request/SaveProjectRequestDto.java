package com.kwcapstone.Domain.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveProjectRequestDto {
    private String projectId;
    private String scription;
    private MultipartFile record;
    private MultipartFile node;
}
