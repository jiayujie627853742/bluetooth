package com.wmty.bluetooth.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * uflo
 * Created by Jack on 2017-11-07.
 */

public class BluetoothRequestBean {

    private String order_id;
    private String order_name;
    private Object param;

    private BluetoothRequestBean() {

    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public String getOrder_name() {
        return order_name;
    }

    public void setOrder_name(String order_name) {
        this.order_name = order_name;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public static class Builder {

        private BluetoothRequestBean mBean;
        private Map<String, Object> mMap;

        public Builder(String orderid, String ordername) {
            mBean = new BluetoothRequestBean();
            mMap = new HashMap<>();
            mBean.setOrder_id(orderid);
            mBean.setOrder_name(ordername);
            mBean.setParam(mMap);
        }

        public Builder(String ordername) {
            mBean = new BluetoothRequestBean();
            mBean.setOrder_name(ordername);
            mBean.setOrder_id(UUID.randomUUID().toString());
            mMap = new HashMap<>();
            mBean.setParam(mMap);
        }

        public Builder setParam(String key, Object param) {
            mMap.put(key, param);
            return this;
        }

        public BluetoothRequestBean build() {
            return mBean;
        }

    }

}
