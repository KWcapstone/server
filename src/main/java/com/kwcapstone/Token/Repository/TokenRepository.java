package com.kwcapstone.Token.Repository;

import com.kwcapstone.Token.Domain.Token;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface TokenRepository extends MongoRepository<Token, ObjectId> {
    Optional<Token> findByRefreshToken (String refreshToken);
    Optional<Token> findByMemberId (ObjectId memberId);
}
