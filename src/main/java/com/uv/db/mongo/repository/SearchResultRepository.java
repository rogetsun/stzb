package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.SearchResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author uvsun 2020/3/11 9:33 下午
 */
@Repository
public interface SearchResultRepository extends MongoRepository<SearchResult, Integer> {
    /**
     * 查询关注了这个 gamer 的 filter
     *
     * @param gamerId
     * @return
     */
    List<SearchResult> findAllByActionGamerIdsContains(String gamerId);
}
