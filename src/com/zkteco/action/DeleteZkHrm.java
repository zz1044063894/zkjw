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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 员工离职删除中控数据
 * @author: JingChu
 * @createtime :2021-01-13 14:00:16
 **/
public class DeleteZkHrm extends BaseBean implements Action {
    String zkKey = this.getPropValue("zkjwMsgOfZK", "ZKTECO_KEY");
    String zkIp = this.getPropValue("zkjwMsgOfZK", "ZKTECO_IP");
    HttpRequestUtils httpRequestUtils = new HttpRequestUtils();

    @Override
    public String execute(RequestInfo requestInfo) {
        String rybm = "";
        String lzyy = "oa人员离职";
        String requestId = requestInfo.getRequestid();
        String billTableName = requestInfo.getRequestManager().getBillTableName();
        String sql = "select * from " + billTableName + " where requestid = " + requestId;

        RecordSet rs1 = new RecordSet();

        rs1.execute(sql);

        if (rs1.next()) {

            rybm = Util.null2String(rs1.getString("rybm"));

        }
        try {
            if (delete2Zk(rybm, lzyy)) {
                writeLog("------------------->INFO:  离职成功");
                return SUCCESS;
            } else {
                writeLog("------------------->INFO:  离职失败");
                requestInfo.getRequestManager().setMessageid("10000");
                requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，中控人员离职失败");
                return FAILURE_AND_CONTINUE;
            }
        } catch (Exception e) {
            writeLog("------------------->INFO:  离职失败" + e.getMessage());
            requestInfo.getRequestManager().setMessageid("10000");
            requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，"+e.getMessage());
            return FAILURE_AND_CONTINUE;
        }

    }

    public Boolean delete2Zk(String rybm, String lzyy) throws Exception {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        String url = "http://" + zkIp + ":8089/api/v2/employee/leave/?key=" + zkKey;
        Map<String, Object> map = new HashMap<>(4);
        map.put("pin", rybm);
        map.put("leavetype", 0);
        map.put("leavedate", date);
        map.put("reason", lzyy);
        String data_xml = JSON.toJSONString(map);
        this.writeLog("------------------->INFO: 本次请求 url ：" + url + "    参数 ：" + data_xml);
        String result = httpRequestUtils.httpPost(url, data_xml);
        JSONObject jsonObj = JSONObject.fromObject(result);
        String ret = jsonObj.getString("ret");
        if ("0".equals(ret)) {
            this.writeLog("------------------->INFO:  本次同步人员规则成功");
            return true;
        } else {
            this.writeLog("------------------->INFO:  本次同步人员规则错误");
            return false;
        }

    }

}
