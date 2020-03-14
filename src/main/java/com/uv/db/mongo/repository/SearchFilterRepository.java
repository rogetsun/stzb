package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.SearchFilter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author uvsun 2020/3/11 9:33 下午
 */
@Repository
public interface SearchFilterRepository extends MongoRepository<SearchFilter, Integer> {

}
