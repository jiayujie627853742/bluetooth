package com.wmty.bluetooth.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.UUID;

import bluetooth.common.BluetoothCode;
import bluetooth.common.BluetoothConnectThread;
import bluetooth.common.BluetoothStatus;

/**
 * uflo
 * Created by Jack on 2017-10-31.
 */

public class BluetoothServerThread extends Thread implements BluetoothCode {

    private final BluetoothServerSocket mServerSocket;

    private final String NAME = "PUFLO";

    private BluetoothStatus mStatus;

    public BluetoothServerThread(@NonNull BluetoothStatus status) {

        mStatus = status;

        BluetoothServerSocket tmp = null;

        UUID uuid = UUID.fromString(key);

        try {
            tmp = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord(NAME, uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mServerSocket = tmp;

    }


    @Override
    public void run() {

        BluetoothSocket socket = null;
        while (true) {
            try {
                mStatus.waitForConnect();
                socket = mServerSocket.accept();
            } catch (IOException e) {
                mStatus.error(e.getMessage());
                break;
            }

            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(socket);
                mStatus.connected();
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

    }

    public void cancel() {
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void manageConnectedSocket(BluetoothSocket socket) {

        BluetoothServerManager.getInstance().cancelDiscovery();

        BluetoothConnectThread bluetoothConnectThread = new BluetoothConnectThread(socket, mStatus);
        BluetoothServerManager.getInstance().setConnectThread(bluetoothConnectThread);
        bluetoothConnectThread.start();
    }

}
