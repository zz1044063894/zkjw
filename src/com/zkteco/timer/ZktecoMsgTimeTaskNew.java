package com.zkteco.timer;

import com.alibaba.fastjson.JSON;
import com.engine.kq.biz.KQFormatBiz;
import com.zkteco.entity.ItemEntity;
import com.zkteco.utils.HttpRequestUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.schedule.BaseCronJob;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @description: 定时获取中控考勤机数据
 * @author: JingChu
 * @createtime :2021-01-12 16:31:27
 **/
public class ZktecoMsgTimeTaskNew extends BaseCronJob {
    BaseBean bb = new BaseBean();
    String zkKey = bb.getPropValue("zkjwMsgOfZK", "ZKTECO_KEY");
    String zkId = bb.getPropValue("zkjwMsgOfZK", "ZKTECO_ID");
    String zkIp = bb.getPropValue("zkjwMsgOfZK", "ZKTECO_IP");
    String hrmBefore = bb.getPropValue("zkjwMsgOfZK", "OA_HRM_BEFROR");
    HttpRequestUtils httpRequestUtils = new HttpRequestUtils();

    /**
     * 获取 uf_kqls 考勤流水中的起始查询id
     */
    @Override
    public void execute() {
        bb.writeLog("------------------->INFO:  定时抓取任务执行开始" + this.getClass().getName());
        //获取当前日期
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        //获取中控上次获取id
        String id = "";
        int startId = -1;
        String sql = "SELECT bckqtbqsid,id FROM uf_kqls ";
        RecordSet rs = new RecordSet();
        rs.execute(sql);
        if (rs.next()) {
            startId = Integer.parseInt(Util.null2String(rs.getString("bckqtbqsid")));
            id = Util.null2String(rs.getString("id"));
        }
        bb.writeLog("------------------->INFO:  " + startId + "  " + id);
        //请求中控数据
        int total = 0;
        int num = 1000;
        while (total % num == 0) {
            int count = 0;
            try {
                String url = "http://" + zkIp + ":8089/api/v2/transaction/get/?key=" + zkKey;
                Map<String, Integer> map = new HashMap<>(2);
                map.put("id", startId);
                map.put("number", num);
                String data_xml = JSON.toJSONString(map);
                bb.writeLog("------------------->INFO: 本次请求 url ：" + url + "    参数 ：" + data_xml);
                String result = httpRequestUtils.httpPost(url, data_xml);
                JSONObject jsonObj = JSONObject.fromObject(result);
                String data = jsonObj.getString("data");
                JSONObject dataJson = JSONObject.fromObject(data);
                bb.writeLog("------------------->INFO: 获取中控数据结果 " + data);
                count = (int) dataJson.get("count");
                if (count == 0) {
                    break;
                }
                JSONArray itemsJson = (JSONArray) dataJson.get("items");
                List<ItemEntity> items = (List<ItemEntity>) JSONArray.toCollection(JSONArray.fromObject(itemsJson), ItemEntity.class);
                String userId = "";
                String checkTime = "";
                int kqId = -1;
                String pin = "";
                String username = "";
                String sn = "";
                int oaId = -1;
                for (int i = 0; i < items.size(); i++) {
                    ItemEntity item = items.get(i);
                    pin = item.getPin();
                    checkTime = item.getChecktime();
                    kqId = item.getId();
                    username = item.getEname();
                    userId = pin;
                    oaId = getOaUserId(userId);
                    sn = item.getSn();
                    insertTzMsg(String.valueOf(oaId), checkTime, kqId, pin, username, date, sn);
                }
                total += count;


            } catch (Exception e) {
                e.getMessage();
            }
        }
        //更新查询到的数据到考勤中间台账
        updateStartId(startId + total, date, "", id, total);
        //更新数据到考勤表
        saveHrmSchedulesignToDB();
    }

    /**
     * 从中控读取的数据插入考勤台账
     *
     * @param userId    人员id
     * @param checkTime 打卡时间
     * @param kqId      考勤机打卡id
     * @param pin       中控人员id
     * @param username  中控人员姓名
     * @param date      插入时间
     */
    public void insertTzMsg(String userId, String checkTime, int kqId, String pin, String username, String date, String zkId) {
        String tzSql = "INSERT INTO uf_zkjwkqtz " +
                "( kqsj,kqzt, kqry, kqid, kqjbh,zkryid,zkryxm,formmodeid,modedatacreatedate,modedatacreater,modedatacreatertype,modedatacreatetime) VALUES " +
                "('" + checkTime + "',1,'" + userId + "', '" + kqId + "', '" + zkId + "', '" + pin + "','" + username + "',12,'" + date + "','1','0','23:59:59')";//台账sql
        RecordSet rs = new RecordSet();

        /**
         * zkkqDayInfo.setFormmodeid(Constant.GET_ZK_KQ_DAILY_INFO_MOUDLE_ID+"");         //模块id
         *                 zkkqDayInfo.setModedatacreater("1");    //模块创建人id
         *                 zkkqDayInfo.setModedatacreatertype("0");//创建人类型(插入默认值0即可)
         *                 zkkqDayInfo.setModedatacreatedate(TimeUtil.getCurrentDateString()); //创建日期
         *                 zkkqDayInfo.setModedatacreatetime(TimeUtil.getOnlyCurrentTimeString()); //创建时间
         */
        Boolean flag = rs.executeUpdate(tzSql);

        bb.writeLog("------------------->INFO:  台账sql：" + tzSql + " " + flag);
    }

    /**
     * 更新台账考勤同步数据
     *
     * @param tzId    台账id
     * @param hrmflag
     */
    public void updateTzMsgTrue(String tzId, Boolean hrmflag) {
        String tzSql = "UPDATE uf_zkjwkqtz SET kqzt = " + (hrmflag ? "0" : "1") + " WHERE id= " + tzId;//台账sql
        RecordSet rs = new RecordSet();

        Boolean flag = rs.executeUpdate(tzSql);

        bb.writeLog("------------------->INFO:  台账sql：" + tzSql + " " + flag);
    }

    /**
     * 插入考勤底表
     *
     * @param userId    用户id
     * @param signType  考勤类型 1签到，2签退
     * @param checkTime 打卡时间
     * @return 插入结果
     */
    public Boolean insertKqMsg(String userId, String signType, String checkTime) {
        String signDate = checkTime.substring(0, 10);
        String signTime = checkTime.substring(11);
        String kqSql = "INSERT INTO hrmschedulesign " +
                "( USERID, USERTYPE, SIGNTYPE, SIGNDATE, SIGNTIME, clientAddress, ISINCOM, ADDR, ISIMPORT) VALUES " +
                "(" + userId + ", '1', '" + signType + "', '" + signDate + "', '" + signTime + "','" + zkIp + "', '1','" + zkId + "', '1')";//考勤底表sql
        RecordSet rs1 = new RecordSet();
        Boolean flag1 = rs1.executeUpdate(kqSql);
        bb.writeLog("------------------->INFO:  底表sql：" + kqSql + " " + flag1);
        return flag1;
    }

    /**
     * 更改考勤流水起始id
     *
     * @param endId 起始id
     * @param date  上次同步时间
     * @param sn    考勤机
     * @param id    根据id更新
     * @return
     */
    public Boolean updateStartId(int endId, String date, String sn, String id, int total) {
        String sql = "UPDATE uf_kqls SET" +
                " bckqtbqsid =" + endId + ",tbsj='" + date + "',sjly='" + sn + "', tbts=" + total + "" +
                " WHERE id = " + id;
        RecordSet rs = new RecordSet();
        Boolean flag = rs.executeUpdate(sql);
        bb.writeLog("------------------->INFO: 更新考勤流水语句： " + sql + " " + flag);
        return flag;
    }

    /**
     * 获取签到和签退
     *
     * @param nowPin    当前签到id
     * @param beforePin 之前签到id
     * @param kqdate    考勤日期
     * @param flagdate
     * @return
     */
    public String getSignType(String nowPin, String beforePin, String kqdate, String flagdate) {
        String SignType = "";   // 1：签到  2：签退
        bb.writeLog("-----------签到状态判断------------->>" + nowPin + " : " + beforePin + "  --  " + kqdate + " : " + flagdate);
        try {
            if (!"".equals(nowPin) && !"".equals(beforePin)) {
                if (nowPin.equals(beforePin) && kqdate.equals(flagdate)) {
                    SignType = "2";
                } else {
                    SignType = "1";
                }

            }
        } catch (Exception e) {
            bb.writeLog("--------getSignType---------->>" + e);
        }
        return SignType;
    }

    /**
     * 从台账中更新到考勤数据底表
     */
    public void saveHrmSchedulesignToDB() {
        String flag = "-1";
        String flagdate = "2021-01-12";
        Calendar calendar = Calendar.getInstance();//此时获取的是系统当前时间
        String todayDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        calendar.add(Calendar.DATE, -1);    //得到前一天
        String yesterdayDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        String sql = "select * from uf_zkjwkqtz where kqzt = 1 and modedatacreatedate >= '" + yesterdayDate + "' ORDER BY kqid,kqsj";
        RecordSet rs = new RecordSet();
        rs.execute(sql);
        String checktime = "";      //考勤时间
        String pin = "";            //人员编号
        String id = "";             //ip
        String signType = "";  //签到状态  1-签到   2-签退
        while (rs.next()) {
            id = Util.null2String(rs.getString("id"));
            pin = Util.null2String(rs.getString("kqry"));
            checktime = Util.null2String(rs.getString("kqsj"));

            //考虑工号录错的情况
            if ("0".equals(pin)) {
                continue;
            }
            String kqdate = checktime.substring(0, 10);

            //判断是签到还是签退数据  根据考勤日期
            signType = getSignType(pin, flag, kqdate, flagdate);
            flag = pin;
            flagdate = kqdate;

            Boolean resHrm = insertKqMsg(pin, signType, checktime);
            new KQFormatBiz().formatDate(pin, kqdate);
            updateTzMsgTrue(id, resHrm);
        }
    }


    /**
     * 获取oa人员id
     *
     * @param userNo 人员编号
     * @return
     */
    public int getOaUserId(String userNo) {
        String sql = "SELECT id FROM hrmresource WHERE workcode='" + userNo + "'";
        RecordSet rs = new RecordSet();
        rs.execute(sql);
        int userId = -1;
        if (rs.next()) {
            userId = Integer.parseInt(Util.null2String(rs.getString("id")));
        }
        bb.writeLog("------------------->INFO:  人员id：" + userId + "   人员编号：" + userNo);
        return userId;
    }

    /**
     * 角色赋权
     * @param pin
     * @param checktime
     */
    /*public void emPower(String pin,String checktime) {

        String maxid = "";
        String sql = "select  max(id) as maxid from uf_get_zk_kq_info where pin = '"+pin +"' and checktime = '"+checktime+"'";
        RecordSet rs = new RecordSet();
        try {
            boolean execute = rs.execute(sql);
            if (rs.next()){
                maxid = rs.getString("maxid");
                ModeRightInfo ModeRightInfo = new ModeRightInfo();
                ModeRightInfo.setNewRight(true);
                ModeRightInfo.editModeDataShare(1,Constant.GET_ZK_KQ_DAILY_INFO_MOUDLE_ID,Integer.parseInt(maxid));
            }
        }catch (Exception e){
            base.writeLog("------saveLoanMoneyInfo------emPower---------"+e);
        }
    }*/
}
