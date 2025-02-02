package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.Member;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MemberRepository extends MongoRepository<Member, ObjectId> {
    boolean existsByEmail(String email);
}
