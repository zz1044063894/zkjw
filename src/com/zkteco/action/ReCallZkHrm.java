package com.zkteco.action;

import com.alibaba.fastjson.JSON;
import com.zkteco.utils.HttpRequestUtils;
import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @description: 返聘人员
 * @author: JingChu
 * @createtime :2021-01-19 21:35:40
 **/
public class ReCallZkHrm extends BaseBean implements Action {
    String zkKey = this.getPropValue("zkjwMsgOfZK", "ZKTECO_KEY");
    String zkIp = this.getPropValue("zkjwMsgOfZK", "ZKTECO_IP");
    HttpRequestUtils httpRequestUtils = new HttpRequestUtils();

    @Override
    public String execute(RequestInfo requestInfo) {
        writeLog("------------------->INFO:  返聘接口后action开始 " + this.getClass().getName());
        String requestId = requestInfo.getRequestid();
        String billTableName = requestInfo.getRequestManager().getBillTableName();
        String rybm = "";
        String sql = "select * from " + billTableName + " where requestid = " + requestId;

        RecordSet rs1 = new RecordSet();

        rs1.execute(sql);

        if (rs1.next()) {

            rybm = Util.null2String(rs1.getString("rybm"));
            try {
                if(reCall2Zk(rybm)){
                    return SUCCESS;
                }else {
                    writeLog("------------------->INFO:  zk返聘失败");
                    requestInfo.getRequestManager().setMessageid("10000");
                    requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，返聘中控人员失败");
                    return FAILURE_AND_CONTINUE;
                }
            } catch (Exception e) {
                writeLog("------------------->INFO:  zk返聘失败"+e.getMessage());
                requestInfo.getRequestManager().setMessageid("10000");
                requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，" + e.getMessage());
                return FAILURE_AND_CONTINUE;
            }
        }else {
            writeLog("------------------->INFO:  zk返聘失败");
            requestInfo.getRequestManager().setMessageid("10000");
            requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，");
            return FAILURE_AND_CONTINUE;
        }

    }

    public Boolean reCall2Zk(String rybm) throws Exception {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String url = "http://" + zkIp + ":8089/api/v2/employee/leave/?key=" + zkKey;
        Map<String, ArrayList<String>> map = new HashMap<>(4);
        ArrayList<String> arrayList = new ArrayList(1);
        arrayList.add(rybm);
        map.put("userpin", arrayList);
        String data_xml = JSON.toJSONString(map);
        this.writeLog("------------------->INFO: 本次请求 url ：" + url + "    参数 ：" + data_xml);
        String result = httpRequestUtils.httpPost(url, data_xml);
        JSONObject jsonObj = JSONObject.fromObject(result);
        String ret = jsonObj.getString("ret");
        if ("0".equals(ret)) {
            this.writeLog("------------------->INFO:  本次返聘同步人员规则成功");
            return true;
        } else {
            this.writeLog("------------------->INFO:  本次返聘人员规则错误");
            return false;
        }

    }
}
