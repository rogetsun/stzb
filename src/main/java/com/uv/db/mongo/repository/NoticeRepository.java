package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.Notice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author uvsun 2020/3/11 9:41 下午
 */
@Repository
public interface NoticeRepository extends MongoRepository<Notice, String> {
    /**
     * 找到尚未通知的第一条通知记录
     * @param hasNotify
     * @return
     */
    Notice findFirstByHasNotify(boolean hasNotify);
}
