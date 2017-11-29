package com.wmty.bluetooth.common;

import java.util.HashMap;
import java.util.Map;

/**
 * uflo
 * Created by Jack on 2017-11-07.
 */

public class BluetoothResultBean {

    private String order_id;
    private int errno;
    private String msg;
    private Object data;

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public int getErrno() {
        return errno;
    }

    public void setErrno(int errno) {
        this.errno = errno;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public static class Build {
        BluetoothResultBean mBean;

        Map<String, Object> mMap;

        public Build(String order_id, int errno, String msg) {
            mMap = new HashMap<>();
            mBean = new BluetoothResultBean();
            mBean.setOrder_id(order_id);
            mBean.setErrno(errno);
            mBean.setMsg(msg);
            mBean.setData(mMap);
        }

        public Build appendParam(String key, Object value) {
            mMap.put(key, value);
            return this;
        }

        public BluetoothResultBean build() {
            return mBean;
        }

        public static Build create(String order_id, int errno, String msg) {
            return new Build(order_id, errno, msg);
        }
    }
}
