package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.Member;
import com.kwcapstone.Domain.Entity.Project;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, ObjectId> {
    // 특정 사용자가 생성한 프로젝트 조회 (내 회의)
    List<Project> findByCreator(ObjectId creator);

    List<Project> findByProjectIdInOrderByUpdatedAtDesc(List<ObjectId> projectIds);
}
