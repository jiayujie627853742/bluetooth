package com.wmty.bluetooth.common;

/**
 * uflo
 * Created by Jack on 2017-11-08.
 */

public interface BluetoothStatus {

    void clientConnectTimeOut();

    void waitForConnect();

    void connected();

    void disconnected();

    void error(String msg);

    void receivedData(byte[] b);

}
