package com.kwcapstone.Kakao;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TokenRepository extends MongoRepository<Token, ObjectId> {
    Optional<Token> findByRefreshToken (String refreshToken);
}
