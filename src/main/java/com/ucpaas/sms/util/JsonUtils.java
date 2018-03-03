package com.ucpaas.sms.util;
import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Created by xiongfenglin on 2017/10/21.
 *
 * @author: xiongfenglin
 */
/**
 * json字符串工具类
 */
public class JsonUtils {
    private static final Gson gson = new Gson();

    /**
     * 将对象转换成为json字符串
     *
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * 将json字符串转换成为对象
     *
     * @param json
     * @param classOfT
     * @return
     */
    public static <T> T toObject(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}
