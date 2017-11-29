package com.wmty.bluetooth.server;

import android.bluetooth.BluetoothAdapter;
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
import bluetooth.common.BluetoothResultBean;
import bluetooth.common.BluetoothStatus;
import bluetooth.common.BluetoothUnHandleOrder;
import util.Logs;

/**
 * uflo
 * Created by Jack on 2017-11-07.
 */

public class BluetoothServerManager implements BluetoothStatus, BluetoothCode {

    public static final String BLUETOOTH_WAIT_CONNECTED = "com.kqcy.bluetooth.server.waitconnect";
    public static final String BLUETOOTH_CONNECTED = "com.kqcy.bluetooth.server.connected";
    public static final String BLUETOOTH_DISCONECTED = "com.kqcy.bluetooth.server.disconnected";
    public static final String BLUETOOTH_ERROR = "com.kqcy.bluetooth.server.error";

    private static BluetoothServerManager mInstance = null;

    private static Context mContext = null;

    private final ScheduledExecutorService mService;

    private BluetoothConnectThread mConnectThread = null;
    private BluetoothServerThread mBluetoothServerThread = null;

    private final LinkedList<BluetoothRequest> mSendList;

    private final BluetoothQueue mQueue;

    private long mTimeOut;
    private ScheduledFuture mFuture;

    private BluetoothUnHandleOrder mUnHandleOrder;

    private BluetoothManageThread mManageThread;

    private boolean isConnected;

    private BluetoothServerManager() {
        mQueue = new BluetoothQueue();
        mSendList = new LinkedList<>();
        mService = Executors.newSingleThreadScheduledExecutor();
        mTimeOut = 2000;
        isConnected = false;
    }

    public static void init(Context context) {
        mContext = context;
    }

    public synchronized static final BluetoothServerManager getInstance() {
        return mInstance == null ? mInstance = new BluetoothServerManager() : mInstance;
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

    public void setConnectThread(BluetoothConnectThread thread) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
        }
        mConnectThread = thread;
    }

    /**
     * 等待客户端连接
     */
    public void waitConnect() {

        stopConnect();
        mBluetoothServerThread = new BluetoothServerThread(this);
        mBluetoothServerThread.start();
    }

    public void stopConnect() {

        if (mManageThread != null) {
            mManageThread.cancel();
        }

        if (mBluetoothServerThread != null) {
            mBluetoothServerThread.cancel();
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
        }
        isConnected = false;
    }

    public void sendRequestOrder(BluetoothRequestBean sendOrder, boolean isNeedReceived, BluetoothInterface bluetoothInterface) {
        if (!isConnected){
            Intent intent = new Intent(BLUETOOTH_ERROR);
            intent.putExtra("EXTRA","蓝牙未连接");
            mContext.sendBroadcast(intent);
            return ;
        }
        if (sendOrder != null) {
            if (isNeedReceived) {
                BluetoothRequest send = new BluetoothRequest();
                send.setOrderid(sendOrder.getOrder_id());
                send.setTime(System.currentTimeMillis());
                send.setmInterface(bluetoothInterface);
                mSendList.add(send);
                send.timeOut(mService,mSendList,mTimeOut,TimeUnit.SECONDS);

               /* if (mFuture == null) {
                    mFuture = mService.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            long currentTime = System.currentTimeMillis();
                            for (BluetoothRequest bs : mSendList) {
                                if (currentTime - bs.getTime() > mTimeOut) {
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
            }

            mQueue.putRequest(sendOrder);
        }
    }

    public void sendResultOrder(BluetoothResultBean result) {
        if (isConnected && result != null) {
            mQueue.putResult(result);
        }
    }

    @Override
    public void clientConnectTimeOut() {
    }

    @Override
    public void waitForConnect() {
        Logs.d(this, "bluetooth wait for connected!");
        mContext.sendBroadcast(new Intent(BLUETOOTH_WAIT_CONNECTED));
        isConnected = false;
    }

    @Override
    public void connected() {
        isConnected = true;
        mManageThread = new BluetoothManageThread(mQueue, mConnectThread);
        mManageThread.start();
        mContext.sendBroadcast(new Intent(BLUETOOTH_CONNECTED));

        Logs.d(this, "bluetooth connected");
    }

    @Override
    public void disconnected() {
        stopConnect();
        mContext.sendBroadcast(new Intent(BLUETOOTH_DISCONECTED));
        Logs.d(this, "bluetooth disconnected");
    }

    @Override
    public void error(String msg) {
        Logs.d(this, "bluetooth error :" + msg);
        Intent intent = new Intent(BLUETOOTH_ERROR);
        intent.putExtra("EXTRA", msg);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void receivedData(byte[] b) {

        if (b.length > 0) {
            try {
                String s = new String(b, Charset.defaultCharset());
                Logs.d(this, "received data : " + s);
                JSONObject object = new JSONObject(s);
                String order_id = object.getString("order_id");
                boolean isHandle = false;
                for (BluetoothRequest bs : mSendList) {
                    if (bs.getOrderid().equals(order_id)) {
                        isHandle = true;
                        mSendList.remove(bs);
                        bs.getmInterface().responseListener(object);
                        break;
                    }
                }

                if (!isHandle && mUnHandleOrder != null) {
                    mUnHandleOrder.UnHandleOrder(object);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

}
