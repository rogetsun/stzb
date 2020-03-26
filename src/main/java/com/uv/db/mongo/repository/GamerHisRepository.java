package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.GamerHis;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author uvsun 2020/3/26 8:16 下午
 */
public interface GamerHisRepository extends MongoRepository<GamerHis, Integer> {
    GamerHis findByOrderSn(String orderSn);

}
