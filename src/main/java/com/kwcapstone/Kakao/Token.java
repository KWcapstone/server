package com.kwcapstone.Kakao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    @Id
    private ObjectId id;
    private String refreshToken;
    private ObjectId memberId; //memberId 참조
}
