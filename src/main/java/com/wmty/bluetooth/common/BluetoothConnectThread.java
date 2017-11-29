package com.wmty.bluetooth.common;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * uflo
 * Created by Jack on 2017-10-31.
 */

public class BluetoothConnectThread extends Thread implements BluetoothManageThreadInterface {

    private final BluetoothSocket mSocket;
    private final InputStream mInputStream;
    private final OutputStream mOutputStream;
    private final Handler mHandler;
    private final ByteArrayOutputStream mBaos = new ByteArrayOutputStream();
    private final BluetoothStatus mStatus;

    public BluetoothConnectThread(BluetoothSocket socket, @NonNull BluetoothStatus status) {
        mSocket = socket;
        mStatus = status;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        mHandler = new Handler(Looper.getMainLooper());

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mInputStream = tmpIn;
        mOutputStream = tmpOut;


    }

    private final Runnable mDataCallback = new Runnable() {
        @Override
        public void run() {
            byte[] bytes = mBaos.toByteArray();
            if (bytes != null && bytes.length > 0) {
                mStatus.receivedData(bytes);
                mBaos.reset();
            }
        }
    };

    @Override
    public void run() {

        byte[] buffer = new byte[1024];
        int bytes;


        while (true) {
            try {
                bytes = mInputStream.read(buffer);

                mHandler.removeCallbacks(mDataCallback);

                mBaos.write(buffer, 0, bytes);

                //延迟10毫秒
                mHandler.postDelayed(mDataCallback, 10);

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        mStatus.disconnected();
    }

    /* Call this from the main activity to send data to the remote device */
    @Override
    public void write(byte[] bytes) {
        try {
            mOutputStream.write(bytes);

        } catch (IOException e) {
            e.printStackTrace();
            mStatus.error(e.getMessage());
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            if (mInputStream != null) mInputStream.close();
            if (mOutputStream != null) mOutputStream.close();
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
