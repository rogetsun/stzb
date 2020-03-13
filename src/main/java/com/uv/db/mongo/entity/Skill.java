package com.uv.db.mongo.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * @author uvsun 2020/3/14 2:15 上午
 * {
 * "name":"衣带密诏",
 * "skill_type":3,
 * "skill_id":200001
 * }
 */
@Data
@Builder
public class Skill {
    @Id
    private int id;
    private String name;
    private int skillType;
    private int skillId;
}
