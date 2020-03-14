package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.Hero;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author uvsun 2020/3/11 9:41 下午
 */
@Repository
public interface HeroRepository extends MongoRepository<Hero, Integer> {
    List<Hero> findAllByQuality(int qualty);
}
