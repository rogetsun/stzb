package com.uv.db.mongo.repository;

import com.uv.db.mongo.entity.SearchFilter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author uvsun 2020/3/11 9:33 下午
 */
@Repository
public interface SearchFilterRepository extends MongoRepository<SearchFilter, String> {
    /**
     * 查找价格大于等于的
     *
     * @param price 筛选价格
     * @return 所有searchFilter
     */
    List<SearchFilter> findByMaxPriceGreaterThanEqual(int price);

    /**
     * 查询关注了这个 gamer 的 filter
     *
     * @param gamerId
     * @return
     */
    List<SearchFilter> findAllByActionGamerIdsContains(String gamerId);
}
