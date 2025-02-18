package com.kwcapstone.Domain.Entity;

import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Builder  // 어.. 필요한가...?
@Document(collection = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    @Id
    private ObjectId memberId;
    private String name;
    private String email;

    @Setter
    private String password;

    @Setter
    private boolean agreement;
    private String image;  // 프로필 이미지 링크
    private String socialId;  // 소셜 로그인에서 제공하는 ID

    @Setter
    private MemberRole role;  // 소셜 로그인 사용자

    @Builder.Default
    private String status = "ACTIVE";  // 계정 활성화 상태 - 수정했는데 필요없는???

    private LocalDateTime inactivationDate;
    private List<ObjectId> projectIds;  // 사용자가 속한 프로젝트 리스트

    @Builder
    public Member(MemberRequestDto memberRequestDto) {
        this.name = memberRequestDto.getName();
        this.email = memberRequestDto.getEmail();
        this.password = memberRequestDto.getPassword();
        this.agreement = memberRequestDto.isAgreement();
        this.role = MemberRole.USER;
    }

    // 구글 로그인 회원가입
    @Builder
    public Member(String name, String email, String socialId, String image, Boolean agreement, String role) {
        this.name = name;
        this.email = email;
        this.socialId = socialId;
        this.image = image;
        this.role = (this.role != null) ? this.role : MemberRole.GOOGLE;
        this.agreement = agreement;
    }

    public Member update(String name, String image) {
        this.name = name;
        this.image = image;

        if (this.role == null) {
            this.role = MemberRole.USER;
        }

        return this;
    }

    public String getRoleKey() {
        return this.role.getKey();
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
