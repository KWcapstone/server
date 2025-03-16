package com.kwcapstone.Repository;

import com.kwcapstone.Domain.Entity.Notice;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends MongoRepository<Notice, ObjectId> {
    // 모든 알림 (최신순 정렬)
    List<Notice> findByUserIdOrderByCreateAtDesc(ObjectId userId);

    // 읽지 않은 알림만 (최신순 정렬)
    List<Notice> findByUserIdAndIsReadFalseOrderByCreateAtDesc(ObjectId userId);
}
