package com.kwcapstone.Token.Domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "token")
@Getter
@NoArgsConstructor
public class Token {
    @Id
    private ObjectId id;
    private String refreshToken;
    private ObjectId memberId; //memberId 참조

    @Builder
    public Token(String refreshToken, ObjectId memberId){
        this.refreshToken = refreshToken;
        this.memberId = memberId;
    }

    public void changeRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }
}
