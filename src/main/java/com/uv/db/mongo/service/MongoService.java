package com.uv.db.mongo.service;

import com.uv.db.mongo.entity.Gamer;
import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.repository.GamerRepository;
import com.uv.db.mongo.repository.SearchFilterRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author uvsun 2020/3/11 9:24 下午
 */
@Service
public class MongoService {
    @Resource
    private SearchFilterRepository searchFilterRepository;

    @Resource
    private GamerRepository gamerRepository;


    // SearchFilter 部分

    public List<SearchFilter> getAllSearchFilter() {
        return searchFilterRepository.findAll();
    }

    public SearchFilter getSearchFilterById(String id) {
        return searchFilterRepository.findById(id).orElse(null);
    }

    public SearchFilter saveSearchFilter(SearchFilter filter) {
        return searchFilterRepository.save(filter);
    }

    public void delAllSearchFilter() {
        searchFilterRepository.deleteAll();
    }

    public List<SearchFilter> getMaxPriceGreatThan(int price) {
        return searchFilterRepository.findByMaxPriceGreaterThanEqual(price);
    }

    // Gamer 部分

    public Gamer saveGamer(Gamer gamer) {
        return gamerRepository.save(gamer);
    }

    public Gamer findGamerBySn(String sn) {
        Optional<Gamer> o = gamerRepository.findById(sn);
        return o.orElse(null);
    }

    public void delAllGamer() {
        gamerRepository.deleteAll();
    }

    public Gamer findGamerById(String sn) {
        return this.findGamerBySn(sn);
    }


}
