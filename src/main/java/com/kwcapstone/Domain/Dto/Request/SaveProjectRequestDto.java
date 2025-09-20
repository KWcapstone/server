package com.kwcapstone.Domain.Dto.Request;

import com.kwcapstone.Domain.Dto.Response.SaveScriptDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveProjectRequestDto {
    private String projectId;
    private String scripion;
    private MultipartFile record;
    private String recordLength;
    private MultipartFile node;
}
