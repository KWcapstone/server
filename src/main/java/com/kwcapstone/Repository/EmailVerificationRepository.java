package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.EmailVerification;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface EmailVerificationRepository extends MongoRepository<EmailVerification, ObjectId> {
    @Aggregation(pipeline = {"{ $match:  {'email': ?0 }}", "{ $sort : {'expirationTime': -1}}", "{ $limit:  1}"})
    Optional<EmailVerification> findLatestByEmail(String email);
}
