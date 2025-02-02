package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.Notice;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NoticeRepository extends MongoRepository<Notice, ObjectId> {
}
