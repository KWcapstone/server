package com.kwcapstone.Token.Domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "token")
@Getter
@NoArgsConstructor
public class Token {
    @Id
    private ObjectId id;

    private String accessToken;
    private String refreshToken;
    private String socialAccessToken;

    private ObjectId memberId; //memberId 참조

    @Builder
    public Token(String accessToken, String refreshToken, ObjectId memberId,String socialAccessToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.memberId = memberId;
        this.socialAccessToken = socialAccessToken;
    }

    public void changeToken(String accessToken, String refreshToken, String socialAccessToken){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.socialAccessToken = socialAccessToken;
    }
}
