package com.kwcapstone.Repository;

import com.kwcapstone.Token.Domain.Token;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends MongoRepository<Token, ObjectId>{
    void deleteByMemberId(ObjectId memberId);
}
