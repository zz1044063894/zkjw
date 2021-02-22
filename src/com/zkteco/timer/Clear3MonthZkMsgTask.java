package com.zkteco.timer;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.interfaces.schedule.BaseCronJob;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @description: 每个月清空一次3个月之前的考勤台账数据
 * @author: JingChu
 * @createtime :2021-01-12 17:08:28
 **/
public class Clear3MonthZkMsgTask extends BaseCronJob {


    @Override
    public void execute() {
        BaseBean baseBean = new BaseBean();
        Date dNow = new Date();   //当前时间

        baseBean.writeLog("---------考勤机数据清理-------start-------->>");

        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //得到日历
        calendar.setTime(dNow);//把当前时间赋给日历
        calendar.add(Calendar.MONTH, -3);  //设置为前3月
        dBefore = calendar.getTime();   //得到前3月的时间
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //设置时间格式
        String defaultStartDate = sdf.format(dBefore);    //格式化前3月的时间

        String sql = "delete uf_zkjwkqtz where kqsj < '"+defaultStartDate+"'";
        RecordSet rs = new RecordSet();
        boolean execute = rs.execute(sql);
        baseBean.writeLog("---------考勤机数据清理--------end------->>" +sql +"   "+execute);

    }


}