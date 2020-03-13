package com.uv.db.mongo.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * @author uvsun 2020/3/14 2:14 上午
 * {
 * "country":"1",
 * "icon_hero_id":100001,
 * "pinyin":"xiandi",
 * "name":"献帝",
 * "season":"N",
 * "hero_type":1,
 * "hero_id":100001,
 * "quality":4
 * }
 */
@Data
@Builder
public class Hero {
    @Id
    private int id;
    private int heroId;
    private String country;
    private String name;
    private String season;
    private int heroType;
    private int quality;
    private String pinyin;
    private int iconHeroId;
}
