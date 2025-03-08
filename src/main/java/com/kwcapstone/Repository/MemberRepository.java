package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.Member;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, ObjectId> {
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNameAndEmail(String name, String email);

    Optional<Member> findBySocialId(String socialId);
}
