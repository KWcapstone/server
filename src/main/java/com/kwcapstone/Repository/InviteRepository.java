package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.Invite;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepository extends MongoRepository<Invite, ObjectId> {
    Optional<Invite> findByInviteCode(String code);
    Optional<Invite> findByProjectId(String projectId);
}
