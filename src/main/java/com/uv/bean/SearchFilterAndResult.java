package com.uv.bean;

import com.uv.db.mongo.entity.SearchFilter;
import com.uv.db.mongo.entity.SearchResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author uvsun 2020/3/14 2:27 下午
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchFilterAndResult {
    private SearchFilter searchFilter;
    private SearchResult searchResult;
}
