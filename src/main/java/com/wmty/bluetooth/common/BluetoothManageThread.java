package com.wmty.bluetooth.common;

import android.util.Log;

import java.io.UnsupportedEncodingException;

/**
 * uflo
 * Created by Jack on 2017-11-08.
 */

public class BluetoothManageThread extends Thread {

    private boolean isExeucte;
    private BluetoothQueue mQueue;
    private BluetoothManageThreadInterface mThread;

    public BluetoothManageThread(BluetoothQueue queue, BluetoothManageThreadInterface thread) {
        isExeucte = false;
        mQueue = queue;
        mThread = thread;
    }

    @Override
    public void run() {

        while (!isExeucte) {
            String bluetoothRequestBean = mQueue.get();
            Log.d(BluetoothManageThread.class.getSimpleName(), "---send data : " + bluetoothRequestBean);
            if (bluetoothRequestBean != null) {
                try {
                    mThread.write(bluetoothRequestBean.getBytes("UTF-8"));
                    Thread.sleep(50);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void cancel() {
        isExeucte = true;
    }

}
