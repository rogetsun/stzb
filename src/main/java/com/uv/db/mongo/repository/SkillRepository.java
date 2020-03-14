package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.Hero;
import com.uv.db.mongo.entity.Skill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author uvsun 2020/3/11 9:41 下午
 */
@Repository
public interface SkillRepository extends MongoRepository<Skill, Integer> {

}
