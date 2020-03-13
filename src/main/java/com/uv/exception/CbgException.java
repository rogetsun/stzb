package com.uv.exception;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author uvsun 2020/3/12 11:28 下午
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CbgException extends Exception {
    private int status;
    private String msg;
    private String url;
    private JSONObject param;

    @Override
    public String getMessage() {
        return "查询藏宝阁失败," + this.toString();
    }

    @Override
    public String getLocalizedMessage() {
        return this.getMessage();
    }
}
