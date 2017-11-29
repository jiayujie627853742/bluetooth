package com.wmty.bluetooth.common;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * uflo
 * Created by Jack on 2017-11-07.
 */

public class BluetoothQueue {

    private LinkedBlockingQueue<String> mQueue;

    public BluetoothQueue() {
        mQueue = new LinkedBlockingQueue<>();
    }

    public synchronized void put(@NonNull String sendBean) {
        mQueue.add(sendBean);
    }

    public void putRequest(BluetoothRequestBean requestBean) {
        if (requestBean != null) {
            put(new Gson().toJson(requestBean).toString());
        }
    }

    public void putResult(BluetoothResultBean resultBean) {
        if (resultBean != null) {
            put(new Gson().toJson(resultBean).toString());
        }
    }

    public String get() {
        try {
            return mQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
