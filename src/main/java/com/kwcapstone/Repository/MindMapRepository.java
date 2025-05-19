package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.MindMap;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MindMapRepository extends MongoRepository<MindMap, ObjectId> {

}
