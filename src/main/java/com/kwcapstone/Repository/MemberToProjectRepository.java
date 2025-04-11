package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.MemberToProject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberToProjectRepository extends MongoRepository<MemberToProject, ObjectId> {
    // 특정 멤버가 초대된 프로젝트 ID 목록 조회(creator 제외)
    List<MemberToProject> findByMemberId(ObjectId memberId);

    void deleteByProjectId(ObjectId projectId);

    //프로젝트 참여자 목록
    List<MemberToProject> findByProjectId(ObjectId projectId);

    //참여중인가요?
    Boolean existsByProjectIdAndMemberId(ObjectId projectId);
}
