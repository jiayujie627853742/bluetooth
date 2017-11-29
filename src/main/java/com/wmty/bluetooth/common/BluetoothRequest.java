package com.wmty.bluetooth.common;


import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * uflo
 * Created by Jack on 2017-11-08.
 */

public class BluetoothRequest {

    private BluetoothInterface mInterface;

    private long time;

    private String orderid;

    private Future mFuture;

    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
    }

    public BluetoothInterface getmInterface() {
        return mInterface;
    }

    public void setmInterface(BluetoothInterface mInterface) {
        this.mInterface = mInterface;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void timeOut(ScheduledExecutorService service, final LinkedList<BluetoothRequest> list, long timeout, TimeUnit timeUnit) {
        mFuture = service.schedule(new Runnable() {
            @Override
            public void run() {
                if (mInterface != null) {
                    mInterface.errorListener(BluetoothCode.TIMEOUT, "请求超时");
                }
                list.remove(BluetoothRequest.this);

            }
        }, timeout, timeUnit);
    }

}
