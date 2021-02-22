package com.zkteco.entity;


/**
 * @description: 考勤数据
 * @author: JingChu
 * @createtime :2021-01-13 15:08:40
 **/
public class ItemEntity {
    private  int id;
    private String pin;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getEname() {
        return ename;
    }

    public void setEname(String ename) {
        this.ename = ename;
    }

    public String getDepartname() {
        return departname;
    }

    public void setDepartname(String departname) {
        this.departname = departname;
    }

    public String getChecktime() {
        return checktime;
    }

    public void setChecktime(String checktime) {
        this.checktime = checktime;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getVerify() {
        return verify;
    }

    public void setVerify(int verify) {
        this.verify = verify;
    }

    public String getStatenol() {
        return statenol;
    }

    public void setStatenol(String statenol) {
        this.statenol = statenol;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    private String ename;
    private String departname;
    private String checktime;
    private String sn;
    private String alias;
    private int verify;
    private String statenol;
    private String state;
}
