package com.wmty.bluetooth.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntRange;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import bluetooth.common.BluetoothCode;
import bluetooth.common.BluetoothConnectThread;
import bluetooth.common.BluetoothInterface;
import bluetooth.common.BluetoothManageThread;
import bluetooth.common.BluetoothQueue;
import bluetooth.common.BluetoothRequest;
import bluetooth.common.BluetoothRequestBean;
import bluetooth.common.BluetoothStatus;
import bluetooth.common.BluetoothUnHandleOrder;
import util.Logs;

/**
 * uflo
 * Created by Jack on 2017-11-08.
 */

public class BluetoothClientManager implements BluetoothStatus, BluetoothCode {

    public static final String BLUETOOTH_CONNECTED = "com.kqcy.bluetooth.connected";
    public static final String BLUETOOTH_DISCONECTED = "com.kqcy.bluetooth.disconnected";
    public static final String BLUETOOTH_TIMEOUT = "com.kqcy.bluetooth.timeout";
    public static final String BLUETOOTH_ERROR = "com.kqcy.bluetooth.error";
    public static final String BLUETOOTH_CONNECTING = "com.kqcy.bluetooth.connecting";
    public static final String BLUETOOTH_NOCONNECTED = "com.kqcy.bluetooth.noconnected";

    private static BluetoothClientManager mInstance = null;

    private static Thread mThread = null;

    private static Context mContext = null;

    private final ScheduledExecutorService mService;

    private BluetoothConnectThread mConnectThread = null;
    private BluetoothClientThread mBluetoothClientThread = null;

    private final LinkedList<BluetoothRequest> mSendList;

    private final BluetoothQueue mQueue;

    private long mTimeOut;
    private ScheduledFuture mFuture;

    private BluetoothUnHandleOrder mUnHandleOrder;

    private BluetoothManageThread mManageThread;

    private boolean isConnected;

    private BluetoothClientManager() {
        mThread = new Thread();
        mQueue = new BluetoothQueue();
        mSendList = new LinkedList<>();
        mService = Executors.newSingleThreadScheduledExecutor();
        mTimeOut = 2000;
        isConnected = false;
    }

    public static void init(Context context) {
        mContext = context;
    }

    public synchronized static final BluetoothClientManager getInstance() {
        return mInstance == null ? mInstance = new BluetoothClientManager() : mInstance;
    }


    public void setRequestTimeOut(@IntRange(from = 2, to = 255) int time) {
        mTimeOut = time * 1000;
    }

    public void setUnHandleOrder(BluetoothUnHandleOrder unHandleOrder) {
        mUnHandleOrder = unHandleOrder;
    }

    /**
     * 启用发现检测
     */
    public void startDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
            discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(discoverableIntent);
        }
    }

    /**
     * 停用发现检测
     */
    public void cancelDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnectThread(BluetoothConnectThread thread) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
        }
        mConnectThread = thread;
    }

    /**
     * 连接服务端
     */
    public void connectServer(BluetoothDevice device) {
        closeConnect();
        mBluetoothClientThread = new BluetoothClientThread(device, this);
        mBluetoothClientThread.start();
    }

    public void closeConnect() {
        if (mManageThread != null) {
            mManageThread.cancel();
        }
        if (mBluetoothClientThread != null) {
            mBluetoothClientThread.cancel();
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
        }
    }

    public void sendOrder(BluetoothRequestBean sendOrder, boolean isNeedReceived, BluetoothInterface bluetoothInterface) {

        if (!isConnected) {
            mContext.sendBroadcast(new Intent(BLUETOOTH_NOCONNECTED));
            if (bluetoothInterface != null) {
                bluetoothInterface.errorListener(997, "蓝牙未连接");
            }
            return;
        }

        if (sendOrder != null) {
            if (isNeedReceived) {
                sendOrder(sendOrder, 1, TimeUnit.SECONDS, bluetoothInterface);
            } else {
                mQueue.putRequest(sendOrder);
            }
        }
    }

    public void sendOrder(BluetoothRequestBean sendOrder, final long timeout, final TimeUnit timeUnit, BluetoothInterface bluetoothInterface) {

        if (!isConnected) {
            mContext.sendBroadcast(new Intent(BLUETOOTH_NOCONNECTED));
            if (bluetoothInterface != null) {
                bluetoothInterface.errorListener(997, "蓝牙未连接");
            }
            return;
        }

        if (sendOrder != null) {
            BluetoothRequest send = new BluetoothRequest();
            send.setOrderid(sendOrder.getOrder_id());
            send.setTime(System.currentTimeMillis());
            send.setmInterface(bluetoothInterface);
            mSendList.add(send);
            send.timeOut(mService, mSendList, timeout, timeUnit);

            /*if (mFuture == null) {
                mFuture = mService.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        long currentTime = System.currentTimeMillis();
                        for (BluetoothRequest bs : mSendList) {
                            if (currentTime - bs.getTime() > timeUnit.toMillis(timeout)) {
                                bs.getmInterface().errorListener(TIMEOUT, "请求超时");
                                mSendList.remove(bs);
                            }
                        }

                        if (mSendList.size() == 0) {
                            mFuture.cancel(true);
                            mFuture = null;
                        }
                    }
                }, 0, 1, TimeUnit.SECONDS);
            }*/

            mQueue.putRequest(sendOrder);
        }
    }

    @Override
    public void clientConnectTimeOut() {
        Logs.d(this, "bluetooth connect  timeout");
        mContext.sendBroadcast(new Intent(BLUETOOTH_TIMEOUT));
        isConnected = false;
    }

    @Override
    public void waitForConnect() {
        Logs.d(this, "bluetooth wait for connect");
        mContext.sendBroadcast(new Intent(BLUETOOTH_CONNECTING));
        isConnected = false;
    }

    @Override
    public void connected() {
        isConnected = true;
        mManageThread = new BluetoothManageThread(mQueue, mConnectThread);
        mManageThread.start();

        Logs.d(this, "bluetooth connected");
        mContext.sendBroadcast(new Intent(BLUETOOTH_CONNECTED));
    }

    @Override
    public void disconnected() {
        isConnected = false;
        closeConnect();
        Logs.d(this, "bluetooth disconnected");
        mContext.sendBroadcast(new Intent(BLUETOOTH_DISCONECTED));
    }

    @Override
    public void error(String msg) {
        Logs.d(this, "bluetooth error");
        Intent intent = new Intent(BLUETOOTH_ERROR);
        intent.putExtra("EXTRA", msg);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void receivedData(byte[] b) {

        if (b.length > 0) {
            try {
                String s = new String(b, Charset.defaultCharset());
                Logs.d(this, "----received data : " + s);
                JSONObject object = new JSONObject(s);
                String order_id = object.getString("order_id");
                boolean isHandle = false;
                for (BluetoothRequest bs : mSendList) {
                    if (bs.getOrderid().equals(order_id)) {
                        isHandle = true;
                        mSendList.remove(bs);
                        if (object.getInt("errno") == SUCCESS) {
                            bs.getmInterface().responseListener(object.optJSONObject("data"));
                        } else {
                            bs.getmInterface().errorListener(object.getInt("errno"), object.getString("msg"));
                        }
                        break;
                    }
                }

                if (!isHandle) {
                    mUnHandleOrder.UnHandleOrder(object);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

}
