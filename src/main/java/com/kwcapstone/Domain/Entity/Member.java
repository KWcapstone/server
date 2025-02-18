package com.kwcapstone.Domain.Entity;

import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    private ObjectId memberId;
    private String name;
    private String email;
    private String password;
    private boolean agreement; //약관 동의 증거
    private String image; //profile
    private String socialId; //social Id
    private String status; //회원 상태
    private LocalDateTime inactivationDate;
    private List<ObjectId> projectIds;  // 사용자가 속한 프로젝트 리스트

    @Builder
    public Member(MemberRequestDto memberRequestDto) {
        this.name = memberRequestDto.getName();
        this.email = memberRequestDto.getEmail();
        this.password = memberRequestDto.getPassword();
        this.agreement = memberRequestDto.isAgreement();
    }

    @Builder
    public Member(String socialId, String name,
                  String imageUrl,String email, Boolean agreement) {
        this.socialId = socialId;
        this.name = name;
        this.image = imageUrl;
        this.email = email;
        this.agreement = agreement;
    }
}
