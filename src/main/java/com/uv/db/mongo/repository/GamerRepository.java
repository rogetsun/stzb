package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.Gamer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * @author uvsun 2020/3/11 9:41 下午
 */
@Repository
public interface GamerRepository extends MongoRepository<Gamer, String> {
    /**
     * 查询所有更新时间小于指定时间的角色.
     *
     * @param date
     * @return
     */
    List<Gamer> findAllByUpdateTimeBefore(Date date);

    /**
     * 查询所有处理时间小于指定时间的角色,可能已经售出
     *
     * @param date
     * @return
     */
    List<Gamer> findAllByDealTimeBefore(Date date);

    Gamer findGamerByOrderSn(String orderSn);

}
