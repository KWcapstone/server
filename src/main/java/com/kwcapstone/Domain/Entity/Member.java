package com.kwcapstone.Domain.Entity;

import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kwcapstone.Domain.Dto.Request.MemberRequestDto;
import com.kwcapstone.Domain.Dto.Request.ProfileEditRequestDto;
import com.kwcapstone.Token.Domain.Token;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Builder  // 어.. 필요한가...?
@Document(collection = "member")
@Getter
@Setter
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

    //로그인 회원가입
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

    public void changePw(String password){
        this.password = password;
    }

    public void editProfie(ProfileEditRequestDto profileEditRequestDto){
        if(profileEditRequestDto.getName()!= null){
            this.name = profileEditRequestDto.getName();
        }
        if(profileEditRequestDto.getImageUrl() != null){
            this.image = profileEditRequestDto.getImageUrl();
        }
    }

    public String getRoleKey() {
        return this.role.getKey();
    }
}
