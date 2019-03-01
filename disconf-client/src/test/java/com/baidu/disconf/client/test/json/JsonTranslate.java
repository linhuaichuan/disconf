package com.baidu.disconf.client.test.json;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.disconf.client.test.common.BaseSpringTestCase;

/**
 * Gson的测试
 *
 * @author liaoqiqi
 * @version 2014-6-16
 */
public class JsonTranslate extends BaseSpringTestCase {

    @Test
    public void test() {

        Map<String, String> colours = new HashMap<String, String>();
        colours.put("BLACK", "#000000");
        colours.put("RED", "#FF0000");
        colours.put("GREEN", "#008000");
        colours.put("BLUE", "#0000FF");
        colours.put("YELLOW", "#FFFF00");
        colours.put("WHITE", "#FFFFFF");

        //
        // Convert a Map into JSON string.
        //
        String json = JSON.toJSONString(colours);
        System.out.println("json = " + json);

        //
        // Convert JSON string back to Map.
        //
        Map<String, String> map = JSONObject.parseObject(json, HashMap.class);
        for (String key : map.keySet()) {
            System.out.println("map.get = " + map.get(key));
        }
    }
}
