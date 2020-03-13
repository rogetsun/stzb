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

    public List<Gamer> findAllByUpdateTimeBefore(Date date);

}
