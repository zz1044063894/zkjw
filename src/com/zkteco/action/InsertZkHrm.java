package com.zkteco.action;

import com.alibaba.fastjson.JSON;
import com.zkteco.utils.HttpRequestUtils;
import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: 新入职员工同步新增中控数据
 * @author: JingChu
 * @createtime :2021-01-13 13:59:24
 **/
public class InsertZkHrm extends BaseBean implements Action {
    String zkKey = this.getPropValue("zkjwMsgOfZK", "ZKTECO_KEY");
    String zkIp = this.getPropValue("zkjwMsgOfZK", "ZKTECO_IP");
    HttpRequestUtils httpRequestUtils = new HttpRequestUtils();

    @Override
    public String execute(RequestInfo requestInfo) {
        //this.writeLog("------------------->INFO:  进入新入职员工同步新增中控数据方法" + this.getClass().getName());
        HashMap map = generateRybm();
        String rybm = (String) map.get("rybm");
        String before = (String) map.get("before");

        writeLog("------------------->INFO:  获取人员相关信息 " + map.toString());
        String requestId = requestInfo.getRequestid();
        String billTableName = requestInfo.getRequestManager().getBillTableName();
        String username = "";
        String sql = "select * from " + billTableName + " where requestid = " + requestId;

        RecordSet rs1 = new RecordSet();

        rs1.execute(sql);

        if (rs1.next()) {

            username = Util.null2String(rs1.getString("rzygxm"));

        }
        String updateSql = "UPDATE " + billTableName + " SET rybm = '" + rybm + "' WHERE requestid = " + requestId;
        RecordSet rs = new RecordSet();
        Boolean flag = rs.execute(updateSql);
        if (flag) {
            try {
                if (save2Zk(rybm, before, username, "1")) {
                    writeLog("------------------->INFO:  同步zk人员成功");
                } else {
                    writeLog("------------------->INFO:  同步zk人员失败");
                    requestInfo.getRequestManager().setMessageid("10000");
                    requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，同步zk人员信息失败");
                    return FAILURE_AND_CONTINUE;
                }
            } catch (Exception e) {
                writeLog("------------------->INFO:  同步人员失败" + e.getMessage());
                requestInfo.getRequestManager().setMessageid("10000");
                requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，" + e.getMessage());
                return FAILURE_AND_CONTINUE;
            }
        }else {
            writeLog("------------------->INFO:  插入人员失败");
            requestInfo.getRequestManager().setMessageid("10000");
            requestInfo.getRequestManager().setMessagecontent("请联系系统管理员，更新人员编码失败败");
            return FAILURE_AND_CONTINUE;
        }
        writeLog("------------------->INFO:  同步人员成功");
        return SUCCESS;
    }

    /**
     * 生成人员编码
     *
     * @return
     */
    public HashMap<String, String> generateRybm() {
        writeLog("------------------->INFO:  进入生成人员编码");
        HashMap<String, String> map = new HashMap<>(4);
        String sql = "SELECT * FROM uf_rybm_zk";
        String rybm = "";
        RecordSet rs = new RecordSet();
        rs.execute(sql);
        String id = "";
        String zkIndex = "";
        String zkBefore = "";
        String zkBhcd = "";
        if (rs.next()) {
            id = Util.null2String(rs.getString("id"));
            zkIndex = Util.null2String(rs.getString("zk_index"));
            zkBefore = Util.null2String(rs.getString("zk_before"));
            zkBhcd = Util.null2String(rs.getString("zk_bhcd"));
        }
        int index = Integer.parseInt(zkIndex);
        int mx = Integer.parseInt(zkBhcd) - zkBefore.length();
        rybm = zkBefore + String.format("%0" + mx + "d", index);
        this.writeLog("------------------->INFO: 生成人员编号 " + rybm);
        map.put("rybm", rybm);
        map.put("before", zkBefore);
        map.put("bhcd", zkBhcd);
        map.put("index", zkIndex);
        index++;
        String updateSql = "UPDATE uf_rybm_zk SET zk_index=" + index + " WHERE id=" + id;
        RecordSet udpateSet = new RecordSet();
        Boolean flag = udpateSet.execute(updateSql);
        this.writeLog("------------------->INFO:  更改编号下标sql<" + updateSql + ">" + flag);
        return map;
    }


    /**
     * @param rybm       人员编码
     * @param before     前缀（备用）
     * @param username   用户姓名
     * @param deptnumber 部门id
     * @return 是否同步成功
     * @throws Exception
     */
    public Boolean save2Zk(String rybm, String before, String username, String deptnumber) throws Exception {

        String url = "http://" + zkIp + ":8089/api/v2/employee/update/?key=" + zkKey;
        Map<String, String> map = new HashMap<>(3);
        map.put("pin", rybm);
        map.put("name", username);
        map.put("deptnumber", deptnumber);
        String data_xml = "[" + JSON.toJSONString(map) + "]";
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
