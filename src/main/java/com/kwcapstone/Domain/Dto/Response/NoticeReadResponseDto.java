package com.kwcapstone.Domain.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoticeReadResponseDto {
    private ObjectId noticeId;
    private String userName;
    private String title;
    private Boolean isRead;
}
