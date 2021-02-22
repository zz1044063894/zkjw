package com.zkteco;

import com.alibaba.fastjson.JSON;
import com.weaverboot.http.httpClient.handle.impl.DefaultPostSendHandle;
import com.weaverboot.http.httpClient.handle.inte.PostSendHandle;
import com.zkteco.entity.ItemEntity;
import com.zkteco.utils.HttpRequestUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: JingChu
 * @createtime :2021-01-13 14:24:37
 **/
public class TestMain {
    public static void main(String[] args) throws Exception {
        /*HttpRequestUtils httpRequestUtils = new HttpRequestUtils();
        int total = 0;
        int num = 5;
        int statid = 1;
        while (total % num == 0) {
            int count = 0;
            //测试循环获取数据
            String url = "http://118.122.32.16:8089/api/v2/transaction/get/?key=0a01w__soou-xfwzh6egwv_awy4xmvgshdlr02v4qc4e";
            Map<String, Integer> map = new HashMap<>(2);
            map.put("id", statid);
            map.put("number", num);
            String data_xml = JSON.toJSONString(map);
            String result = httpRequestUtils.httpPost(url, data_xml);
            JSONObject jsonObj = JSONObject.fromObject(result);
            String data = jsonObj.getString("data");
            JSONObject dataJson = JSONObject.fromObject(data);
            count = (int) dataJson.get("count");
            JSONArray itemsJson = (JSONArray) dataJson.get("items");
            List<ItemEntity> items = (List<ItemEntity>) JSONArray.toCollection(JSONArray.fromObject(itemsJson), ItemEntity.class);
            for(int i=0;i<items.size();i++){
                ItemEntity item = items.get(i);

            }

            count += 0;
            total += count;
            statid += count;
        }*/
        int index = 100;
        int mx = 10 - 4;
        System.out.println("ZKJW"+String.format("%0" + mx + "d", index));
        String a ="ZKJW1231241";
        System.out.println(a.substring(4));
    }
}
