package com.uv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author uvsun 2020/3/12 2:01 上午
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "cbg.return")
public class CbgReturnKey {
    private String paging;
    private String isLastPage;
    private String status;
    private String msg;
    private String result;
    private String num;
    private String icon;

    private String detailGamerKey;

    private String firstPriceKey;
    private String gamerNameKey;
    private String sellStatusKey;
    private String sellStatusNameKey;

    private String detailGamerInfoKey;

    private String detailTenureKey;
    private String detailTenureYuanBaoKey;
    private String detailDianJiKey;
    private String detailDianCangKey;
    private String detailCardKey;
    private String detailCardIdKey;
    private String detailSkillKey;
    private String detailSkillIdKey;
    private String detailCardFeatureKey;




}
