package com.wmty.bluetooth.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import bluetooth.common.BluetoothCode;
import bluetooth.common.BluetoothConnectThread;
import bluetooth.common.BluetoothStatus;

/**
 * uflo
 * Created by Jack on 2017-11-08.
 */

public class BluetoothClientThread extends Thread implements BluetoothCode {

    private final BluetoothSocket mSocket;
    private final BluetoothDevice mDevice;
    private BluetoothStatus mStatus;
    private final ScheduledExecutorService mScheduledExecutorService;
    private int mTimeOut;//default is 5 seconds
    private ScheduledFuture<?> mFuture;

    public BluetoothClientThread(BluetoothDevice device, @NonNull BluetoothStatus status) {
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        BluetoothSocket tmp = null;
        mDevice = device;
        mStatus = status;
        mTimeOut = 5;

        UUID uuid1 = UUID.randomUUID();
        String s = uuid1.toString();
        Log.d("TAG", "Bluetooth uuid : " + s);

        try {
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSocket = tmp;
    }

    public void setTimeOut(int timeOut) {
        mTimeOut = timeOut;
    }

    @Override
    public void run() {

        //cancel discovery because is will show down the connection
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();


        try {

            mFuture = mScheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    timeOut();
                }
            }, mTimeOut, TimeUnit.SECONDS);

            mSocket.connect();

        } catch (IOException e) {
            e.printStackTrace();
            try {
                mSocket.close();
            } catch (IOException e1) {
                e.printStackTrace();
            }
            return;
        }
        Log.d("TAG", "-----1 " + (mSocket.isConnected()));
        manageConnectedSocket(mSocket);
        if (!mFuture.isDone()) {
            mFuture.cancel(true);
        }
        mStatus.connected();
    }

    public void timeOut() {
        try {
            mSocket.close();
            mStatus.clientConnectTimeOut();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        if (mFuture != null && !mFuture.isDone()) {
            mFuture.cancel(true);
        }
        try {
            if (mSocket.isConnected()) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void manageConnectedSocket(BluetoothSocket socket) {

        BluetoothConnectThread thread = new BluetoothConnectThread(socket, mStatus);
        BluetoothClientManager.getInstance().setConnectThread(thread);
        thread.start();

    }

}
